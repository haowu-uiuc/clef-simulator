package largeflow.utils.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import largeflow.utils.StepCurve;
import largeflow.utils.Tuple;

public class StepCurveTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        // init curve
        StepCurve curve = new StepCurve(10.);
        System.out.println(curve);
        assertTrue(curve.endTime() == 10.);
        assertTrue(curve.getJumpPoints().size() == 1);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        
        // illegal case
        curve.addCurve(new Tuple<Double, Integer>(6., 2), 5.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 1);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        
        // illegal case
        curve.addCurve(new Tuple<Double, Integer>(11., 2), 12.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 1);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        
        // add first burst
        curve.addCurve(new Tuple<Double, Integer>(1., 2), 5.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 3);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 2)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(5., 0)));
                
        // add a burst with exact the same start and end time of a existing one
        curve.addCurve(new Tuple<Double, Integer>(1., 1), 5.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 3);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 3)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(5., 0)));
        
        // add a second independent burst
        curve.addCurve(new Tuple<Double, Integer>(8., 1), 8.5);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 5);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 3)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(5., 0)));
        assertTrue(curve.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(8., 1)));
        assertTrue(curve.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(8.5, 0)));
        
        // add a third independent burst but the busrt ends at the end of the curve
        curve.addCurve(new Tuple<Double, Integer>(9., 2), 10.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 7);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 3)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(5., 0)));
        assertTrue(curve.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(8., 1)));
        assertTrue(curve.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(8.5, 0)));
        assertTrue(curve.getJumpPoints().get(5).equals(new Tuple<Double, Integer>(9., 2)));
        assertTrue(curve.getJumpPoints().get(6).equals(new Tuple<Double, Integer>(10., 0)));
        
        // add a burst which ends at a time larger than the end of the curve
        curve.addCurve(new Tuple<Double, Integer>(9., 5), 11.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 7);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 3)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(5., 0)));
        assertTrue(curve.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(8., 1)));
        assertTrue(curve.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(8.5, 0)));
        assertTrue(curve.getJumpPoints().get(5).equals(new Tuple<Double, Integer>(9., 7)));
        assertTrue(curve.getJumpPoints().get(6).equals(new Tuple<Double, Integer>(10., 0)));

        // add a burst whose end time is an existing node
        curve.addCurve(new Tuple<Double, Integer>(3., 1), 5.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 8);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 3)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(3., 4)));
        assertTrue(curve.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(5., 0)));
        assertTrue(curve.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(8., 1)));
        assertTrue(curve.getJumpPoints().get(5).equals(new Tuple<Double, Integer>(8.5, 0)));
        assertTrue(curve.getJumpPoints().get(6).equals(new Tuple<Double, Integer>(9., 7)));
        assertTrue(curve.getJumpPoints().get(7).equals(new Tuple<Double, Integer>(10., 0)));
        
        // add a burst whose start time is an existing node
        curve.addCurve(new Tuple<Double, Integer>(1., 1), 2.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 9);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 4)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(2., 3)));
        assertTrue(curve.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(3., 4)));
        assertTrue(curve.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(5., 0)));
        assertTrue(curve.getJumpPoints().get(5).equals(new Tuple<Double, Integer>(8., 1)));
        assertTrue(curve.getJumpPoints().get(6).equals(new Tuple<Double, Integer>(8.5, 0)));
        assertTrue(curve.getJumpPoints().get(7).equals(new Tuple<Double, Integer>(9., 7)));
        assertTrue(curve.getJumpPoints().get(8).equals(new Tuple<Double, Integer>(10., 0)));
        
        // add a burst which result in duplicated nodes, and we test whether the deduplication works
        curve.addCurve(new Tuple<Double, Integer>(2., 1), 3.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 7);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 4)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(5., 0)));
        assertTrue(curve.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(8., 1)));
        assertTrue(curve.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(8.5, 0)));
        assertTrue(curve.getJumpPoints().get(5).equals(new Tuple<Double, Integer>(9., 7)));
        assertTrue(curve.getJumpPoints().get(6).equals(new Tuple<Double, Integer>(10., 0)));
        
        // add a burst across a part of an existing burst
        curve.addCurve(new Tuple<Double, Integer>(3., 1), 6.);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 9);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 4)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(3., 5)));
        assertTrue(curve.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(5., 1)));
        assertTrue(curve.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(6., 0)));
        assertTrue(curve.getJumpPoints().get(5).equals(new Tuple<Double, Integer>(8., 1)));
        assertTrue(curve.getJumpPoints().get(6).equals(new Tuple<Double, Integer>(8.5, 0)));
        assertTrue(curve.getJumpPoints().get(7).equals(new Tuple<Double, Integer>(9., 7)));
        assertTrue(curve.getJumpPoints().get(8).equals(new Tuple<Double, Integer>(10., 0)));
        
        // add a burst across  multiple existing bursts
        curve.addCurve(new Tuple<Double, Integer>(0.5, 2), 6.5);
        System.out.println(curve);
        assertTrue(curve.getJumpPoints().size() == 11);
        assertTrue(curve.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(0.5, 2)));
        assertTrue(curve.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(1., 6)));
        assertTrue(curve.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(3., 7)));
        assertTrue(curve.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(5., 3)));
        assertTrue(curve.getJumpPoints().get(5).equals(new Tuple<Double, Integer>(6., 2)));
        assertTrue(curve.getJumpPoints().get(6).equals(new Tuple<Double, Integer>(6.5, 0)));
        assertTrue(curve.getJumpPoints().get(7).equals(new Tuple<Double, Integer>(8., 1)));
        assertTrue(curve.getJumpPoints().get(8).equals(new Tuple<Double, Integer>(8.5, 0)));
        assertTrue(curve.getJumpPoints().get(9).equals(new Tuple<Double, Integer>(9., 7)));
        assertTrue(curve.getJumpPoints().get(10).equals(new Tuple<Double, Integer>(10., 0)));
        
        
        // test the case when the endindex is at the end of the existing curve
        // init curve
        StepCurve curve2 = new StepCurve(10.);
        System.out.println(curve2);
        assertTrue(curve2.endTime() == 10.);
        assertTrue(curve2.getJumpPoints().size() == 1);
        assertTrue(curve2.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        
        // add first burst
        curve2.addCurve(new Tuple<Double, Integer>(1., 2), 5.);
                
        // add a burst whose end point is larger then the first one.
        curve2.addCurve(new Tuple<Double, Integer>(3., 1), 6.);
        System.out.println(curve2);
        assertTrue(curve2.getJumpPoints().size() == 5);
        assertTrue(curve2.getJumpPoints().get(0).equals(new Tuple<Double, Integer>(0., 0)));
        assertTrue(curve2.getJumpPoints().get(1).equals(new Tuple<Double, Integer>(1., 2)));
        assertTrue(curve2.getJumpPoints().get(2).equals(new Tuple<Double, Integer>(3., 3)));
        assertTrue(curve2.getJumpPoints().get(3).equals(new Tuple<Double, Integer>(5., 1)));
        assertTrue(curve2.getJumpPoints().get(4).equals(new Tuple<Double, Integer>(6., 0)));
    }

}
