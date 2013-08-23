namespace java thrift  // define namespace for java code

typedef i64 long
typedef i32 int
service ArithmeticService {  // defines simple arithmetic service
            long add(1:int num1, 2:int num2),
            long multiply(1:int num1, 2:int num2),
}