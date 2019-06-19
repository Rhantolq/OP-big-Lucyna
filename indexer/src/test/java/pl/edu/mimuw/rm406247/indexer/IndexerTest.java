package pl.edu.mimuw.rm406247.indexer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        String type1 = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String type2 = "application/vnd.oasis.opendocument.text";
        Assert.assertTrue(type1.contains("openxmlformats"));
        Assert.assertTrue(type2.contains("opendocument.text"));

    }

    private String run(String args[]) {
        outContent.reset();
        IndexerMain.main(args);
        return outContent.toString();
    }
}