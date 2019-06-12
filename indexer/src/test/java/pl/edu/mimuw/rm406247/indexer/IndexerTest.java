package pl.edu.mimuw.rm406247.indexer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class IndexerTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Before
    public void setUp() throws Exception {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void tearDown() throws Exception {
        System.setOut(null);
    }

    @Test
    public void main() {
        Assert.assertTrue(true);
    }

    private String run(String args[]) {
        outContent.reset();
        Indexer.main(args);
        return outContent.toString();
    }
}