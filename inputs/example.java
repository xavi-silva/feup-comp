import io;
class Factorial {
    public int computeFactorial(int num){
        int num_aux ;
        if (num < 1)
            num_aux = 1;
        else
            num_aux = num * (this.computeFactorial(num-1));
        return num_aux;
    }
    public static void main(String[] args){
        io.println(new Factorial().computeFactorial(10)); //assuming the existence
// of the classfile io.class
    }
}