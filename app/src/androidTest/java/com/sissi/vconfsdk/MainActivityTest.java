package com.sissi.vconfsdk;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Sissi on 2018/9/11.
 */
public class MainActivityTest {
    @Before
    public void setUp() throws Exception {
        System.out.println("before test onCreate");
    }

    @Test
    public void onCreate() throws Exception {
        System.out.println("test onCreate");
        assertEquals(6, 3*2);
    }

}