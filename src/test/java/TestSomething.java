public class TestSomething {

    public String map;

    public static void main(String[] args) {
        TestSomething something = new TestSomething();
        String aMap = something.map;
        something.map = "nihao";
        aMap = "tahao";
        System.out.println(something.map);
        System.out.println(aMap);
    }
}
