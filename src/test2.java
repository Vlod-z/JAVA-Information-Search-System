import java.util.Scanner;

public class test2 {
    public static void main(String[] args) {
        try {
            proprocess d = new proprocess();
            d.makeTerms();
            d.makeIndex();
            d.vectorspace();
            System.out.println("请输入查询内容：");
            Scanner input = new Scanner(System.in);
            String search = input.next();
            d.looking(search);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
