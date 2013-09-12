<?php

/**
 * Client class for Phantom.
 *
 * Its been purposefully designed to have only one static method for invoking a command on the agent, as the agent
 * is designed to run only one command per socket and then closed down. Thus, no point in creating a W3AgentClient
 * object when it would get consumed after just one call.
 *
 * The command protocol is defined as follows:
 *
 * <pre>
 *
 * Command is described as below
 * +-------+---------+-------+---------+---+---------+-------+---------+---+---------+-------+-----------------+----+
 * | delim | command | delim | param 1 | = | value 1 | delim | param n | = | value n | delim | data size (int) | \n |
 * +-------+---------+-------+---------+---+---------+-------+---------+---+---------+-------+-----------------+----+
 * +------------+
 * | data bytes |
 * +------------+
 *
 * where
 * <ul>
 *  <li>Command and params appear on a single line terminating in '\n' char</li>
 *	<li>'delim char' is any non-ascii character</li>
 * 	<li>'command' is an arbitrary sequence of characters</li>
 * 	<li>'param'='value' can repeat any number of times. Are of type : arbitrary sequence of characters</li>
 *	<li>'data' is an arbitrary sequence of bytes</li>
 * </ul>
 *
 * Response from Command execution is described as below
 *
 * +--------+----+
 * | status | \n |
 * +--------+----+
 *  (or)
 * +--------+-------------+-------------+----+
 * | status | white space | data length | \n |
 * +--------+-------------+-------------+----+
 * +------------+
 * | data bytes |
 * +------------+
 *
 * </pre>
 *
 * @author Amod Malviya <amod@flipkart.com>
 * @author Kartik Ukhalkar <kartikbu@flipkart.com>
 * @author Arya Ketan <arya.ketan@flipkart.com>
 *
 */
class W3AgentClient {

    private static $delimiters = ' ^!$%@#*~';
    private static $socketDomain = AF_UNIX;
    private static $socketType = SOCK_STREAM;
    private static $socketProtocol = 0;

    /**
     * creates and returns a socket with a timeout
     * @static
     * @param string $address address to connect
     * @param int $port port to connect
     * @param $command the command to be sent after connect
     * @param $params the params to be sent after connect
     * @return boolean true on success, false on failure
     */
    private static function createSocket($socketFile)
    {
        // create the socket
        $sock = socket_create(self::$socketDomain, self::$socketType, self::$socketProtocol);
        if (!$sock) {
            return false;
        }

        // connect to the address
        if (!socket_connect($sock, $socketFile)) {
            socket_close($sock);
            return false;
        }
        return $sock;
    }

    /**
     * creates and returns a command line given command name and key->value params
     * @static
     * @param string $command command name
     * @param array $params list of key->value params
     * @param string $data data sent after the command (if any)
     * @return string the constructed command line
     */
    protected static function getCommandLine($command, $params, $data) {

        // add request id param
        if (isset($_SERVER) && isset($_SERVER["REQUEST_URI"])) {
            $ident = isset($_SERVER['UNIQUE_ID']) ? $_SERVER['UNIQUE_ID'] : urlencode($_SERVER["REQUEST_URI"]);
            if (is_array($params)) {
                $params["requestID"] = $ident;
            } else {
                $params = array("requestID"=>$ident);
            }
        }

        // get the correct delimiter - based on presence of each valid delimiters in parameter value
        $num = strlen(self::$delimiters);
        $delim = ' ';
        if ($params) {
            // try each of the valid delimiters
            for ($i=0; $i<$num; $i++) {
                $delim = self::$delimiters[$i];
                $found = false;
                foreach ($params as $k=>$v) {
                    if (strpos($v, $delim)) {
                        $found = true;
                        break;
                    }
                }
                if (!$found) break;
            }
            // could not determine which delimiter to use
            if ($i == $num) {
                throw new Exception('Could not determine any delimiters');
            }
        }

        // prepend delimeter to command if its not space
        $cmd = ($delim==' ' ? $command : ($delim . $command));

        // create list of params using delimeter separation
        if ($params) {
            foreach ($params as $k=>$v) $cmd .= $delim . $k . '=' . $v;
        }

        // append number of bytes in data after the command
        if ($data) {
            $cmd .= $delim . strlen($data);
        }

        // end of command - append \n before start of data
        $cmd .= "\n";

        // if command length is longer than max, log and fail
        if (strlen($cmd) >= Config::$W3Agent_command_max_length) {
            return false;
        }

        return $cmd;
    }

    /**
     * parses the returned data
     * @static
     * @param resource $sock the read socket
     * @param pointer $response this is where response status is stored
     * @param pointer $respData this is where response data is stored
     * @return boolean status of the command, true if "SUCCESS", false otherwise
     */
    private static function readResponse($sock,&$response,&$respData)
    {

        $returnCode = false;

        // read first few bytes
        $resp = socket_read($sock, 256);
        if ($resp === false) {
            return 0;
        }
        if ($resp === '') {
            return 0;
        }

        // keep reading till we get a \n
        while (strpos($resp, "\n") === false) {
            $newdata = socket_read($sock, 256);
            if ($newdata === false) {
                return 0;
            }
            if ($newdata === '') {
                return 0;
            }
            $resp .= $newdata;
        }

        // parse response and data length first
        $pos = strpos($resp, "\n");
        $dlenpos = $pos;
        while ($resp[$dlenpos-1]>='0' && $resp[$dlenpos-1]<='9') $dlenpos--;

        // this gives the data length
        $dlen = ($dlenpos<$pos) ? intval(substr($resp, $dlenpos, $pos-$dlenpos)) : 0;

        // this gives the response
        $response = substr($resp, 0, $dlenpos);

        // this gives the data
        $respData = null;
        if ($dlen > 0) {
            $respData = substr($resp, $pos+1, strlen($resp)-$pos-1);
            while (strlen($respData) < $dlen) {
                $tmp = socket_read($sock, $dlen-strlen($respData));
                if ($tmp === false) {
                    break;
                }
                if ($tmp === '') {
                    break;
                }
                $respData .= $tmp;
            }
        }

        // return true only if "SUCCESS" is the response
        $returnCode = strpos($response, 'SUCCESS') === 0;

        return $returnCode;

    }

    /**
     * Sends a command to a socket with the specified params and the data
     * @static
     * @param string $command the command to execute
     * @param array $params parameters in the form of key value pairs
     * @param mixed $data the data to be sent - can be null
     * @param string $response the variable in which the response line would be stored
     * @param mixed $respData the response data object
     * @param mixed $socketFile the socket File to connect to
     * @return bool true if the command succeeded, false otherwise
     */
    public static function sendCommand($command, $params, $data, &$response, &$respData, $socketFile)
    {

        $returnCode = 0;
        try
        {
            // create socket
            $sock =  self::createSocket($socketFile);
            if (!$sock)
            {
                return false;
            }

            // get command to send
            if (!$command_str = self::getCommandLine($command, $params, $data)) {
                socket_close($sock);
                return false;
            }

            // write to socket
            if (!socket_write($sock, $command_str.$data)) {
                socket_close($sock);
                return false;
            }

            // parse response
            $returnCode = self::readResponse($sock,$response,$respData,$command,$params);
        }
        catch (Exception $e)
        {
            // Log Exception or throw exception
        }

        // close socket
        socket_close($sock);

        return $returnCode;

    }
}
