package gr.teilar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimplexTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testSimplexObject() {
        String objFun = "50*x1+40*x2";
        List<String> restrictions = new ArrayList<>(Arrays.asList(
                "3*x1+5*x2<=150",
                "x2<=20",
                "8*x1+5*x2<=300")
        );

        /*String objFun = "50*x1+120*x2+40*x3+80*x4";
        List<String> restrictions = new ArrayList<>(Arrays.asList(
                "2*x1+x2+x3<=450",
                "3*x2+x3+x4<=180",
                "4*x1+x3<=400",
                "x1+x2+x4<=110")
        );*/

        Simplex test = null;
        try {
            test = new Simplex(objFun, true, restrictions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("\n" + test + "\n");
    }
}