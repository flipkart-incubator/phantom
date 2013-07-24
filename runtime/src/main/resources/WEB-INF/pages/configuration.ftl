<#include "./../header.ftl"> 
<#import "/spring.ftl" as spring />
<script type="text/javascript">

</script>
<div id="configuration">

	<h1>Service Proxy Configuration</h1>
<!-- 	<#assign url><@spring.url relativeUrl="${servletPath}/job-configuration"/></#assign> -->
		
		<span style="color:red "><#if RequestParameters.Error??>${RequestParameters.Error}</#if> </span>
	
	
	<#if (networkServers?? && networkServers?size!=0) || (UDSServers?? && UDSServers?size!=0) >
	<h2>Deployed Servers</h2>
	<table id = "sp-conf-table-servers" class="bordered-table">
	<tr>
		<th>ServerType</th>
		<th>Endpoint</th>
		<th>Registered Channel Handlers</th>
	</tr>
	<#if networkServers?? && networkServers?size!=0>
	<#list networkServers as nServer>
		<tr>
			<td>${nServer.getServerType()}</td>
			<td>${nServer.getServerEndpoint()}</td>
			<td><#list nServer.pipelineFactory.channelHandlersMap?keys as chkey> ${chkey}, </#list></td>
		</tr>
	</#list>
	</#if>
	</table>
	</#if>
	
	
	<#if taskHandlers?? && taskHandlers?size!=0>
	<h2>Task Handlers</h2>
	<table id = "sp-conf-table" class="bordered-table">
	<tr>
		<th> Handler Name </th>
		<th> Edit </th>
		<th> Registered Commands </th>
	</tr>
	
	<tr>
	<#list taskHandlers as handler>
		<tr>
					<#assign job_url><@spring.url relativeUrl="/viewConfig/handler/${handler.name}"/></#assign>
					<#assign mod_url><@spring.url relativeUrl="/modify/handler/${handler.name}"/></#assign>
					<#assign del_url><@spring.url relativeUrl="/delete/handler/${handler.name}"/></#assign>
					<td><a href="${job_url}">${handler.name}</a></td>
					<td><a href="${mod_url}">Edit</a></td>
					<td><#list handler.commands as command>${command} , </#list></td>
				<!--	<td><a href="${del_url}">Delete</a></td> -->
		</tr>
	</#list>
	</#if>
	</table>
		
	<#if thriftProxies?? && thriftProxies?size!=0>
	<h2>Thrift Proxies</h2>
	<table id = "sp-conf-table-thrift" class="bordered-table">
	<tr>
		<th> Proxy Name </th>
		<th> Thrift Server Name </th>
		<th> Port </th>
	</tr>
	
	<tr>
	
	<#list thriftProxies as proxy>
		<tr>
					<td>${proxy.name}</td>
					<td>${proxy.thriftServer}</td>
					<td>${proxy.thriftPort}</td>
		</tr>
	</#list>
	</#if>
	</table>

</div><!-- configuration -->

<#include "./../footer.ftl"> 

