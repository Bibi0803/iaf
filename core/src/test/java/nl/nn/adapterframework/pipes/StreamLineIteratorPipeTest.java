package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class StreamLineIteratorPipeTest extends IteratingPipeTest<StreamLineIteratorPipe> {

	@Override
	public StreamLineIteratorPipe createPipe() {
		StreamLineIteratorPipe result = new StreamLineIteratorPipe();
		result.setCombineBlocks(false); // default is true, but false is compatible with super test class IteratingPipeTest
		return result;
	}



	public void testBasicWithLinePrefixAndSuffix(boolean blockEnabled, boolean combinedBlocks, String expectedLogFile) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setLinePrefix("{"); 
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(combinedBlocks);
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultWithLineFixes.xml");
		String expectedLog = TestFileUtils.getTestFile(expectedLogFile);
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedLogFile, expectedLog, resultLog.toString().trim());
		assertEquals(expected, actual);
	}

	@Test
	public void testBasicWithLinePrefixAndSuffix() throws Exception {
		testBasicWithLinePrefixAndSuffix(false, false, "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}
	@Test
	public void testBasicWithLinePrefixAndSuffixCombined() throws Exception {
		testBasicWithLinePrefixAndSuffix(false, true, "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}

	@Test
	public void testBasicWithLinePrefixAndSuffixBlockEnabled() throws Exception {
		testBasicWithLinePrefixAndSuffix(true, false, "/IteratingPipe/TenLinesLogPlainWithLineFixesBlockEnabled.txt");
	}

	@Test
	public void testBasicWithLinePrefixAndSuffixBlockEnabledCombined() throws Exception {
		testBasicWithLinePrefixAndSuffix(true, true, "/IteratingPipe/TenLinesLogPlainWithLineFixesBlockEnabled.txt");
	}


	public void testBlocksWithCombine(boolean blockEnabled, boolean combinedBlocks, int blockSize, String expectedFile, String expectedLogFile) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(blockSize);
		pipe.setLinePrefix("{"); 
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(combinedBlocks);
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile(expectedFile);
		String expectedLog = TestFileUtils.getTestFile(expectedLogFile);
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedLogFile, expectedLog, resultLog.toString().trim());
		assertEquals(expectedFile, expected, actual);
	}

	@Test
	public void testFullBlocksWithCombineOff() throws Exception {
		testBlocksWithCombine(false, false, 5, "/IteratingPipe/TenLinesResultWithLineFixes.xml", "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}
	@Test
	public void testFullBlocksWithCombineOn() throws Exception {
		testBlocksWithCombine(false, true, 5, "/IteratingPipe/TenLinesResultCombinedInBlocksOfFiveWithLineFixes.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfFiveWithLineFixes.txt");
	}
	@Test
	public void testFullBlocksWithCombineOffBlockEnabled() throws Exception {
		testBlocksWithCombine(true, false, 5, "/IteratingPipe/TenLinesResultWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogInBlocksOfFiveWithLineFixesBlockEnabled.txt");
	}
	@Test
	public void testFullBlocksWithCombineOnBlockEnabled() throws Exception {
		testBlocksWithCombine(true, true, 5, "/IteratingPipe/TenLinesResultCombinedInBlocksOfFiveWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfFiveWithLineFixesBlockEnabled.txt");
	}

	@Test
	public void testBlocksOf1WithCombineOff() throws Exception {
		testBlocksWithCombine(false, false, 1, "/IteratingPipe/TenLinesResultWithLineFixes.xml", "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}
	@Test
	public void testBlocksOf1WithCombineOn() throws Exception {
		testBlocksWithCombine(false, true, 1, "/IteratingPipe/TenLinesResultCombinedInBlocksOfOneWithLineFixes.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfOneWithLineFixes.txt");
	}

	@Test
	public void testBlocksOf1WithCombineOffBlockEnabled() throws Exception {
		testBlocksWithCombine(true, false, 1, "/IteratingPipe/TenLinesResultWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogInBlocksOfOneWithLineFixesBlockEnabled.txt");
	}
	@Test
	public void testBlocksOf1WithCombineOnBlockEnabled() throws Exception {
		testBlocksWithCombine(true, true, 1, "/IteratingPipe/TenLinesResultCombinedInBlocksOfOneWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfOneWithLineFixesBlockEnabled.txt");
	}

	@Test
	public void testPartialFinalBlockWithLinePrefixAndSuffix() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setBlockSize(4);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultInBlocksOfFour.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testPartialFinalBlockMaxItemsWithLinePrefixAndSuffix() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultInBlocksOfFour.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testBlocksByKey() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setStartPosition(4);
		pipe.setEndPosition(5);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultInKeyBlocks.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testBasicWithoutXmlEscaping() throws Exception {
		pipe.setSender(getElementRenderer(false));
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLinesWithXmlChars.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultWithoutXmlCharsEscaped.txt");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testBasicWithXmlEscaping() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setEscapeXml(true);
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLinesWithXmlChars.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultWithXmlCharsEscaped.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testEndOfLineString() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setEndOfLineString("EOL");
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/StreamLineIteratorPipe/EndMarked.txt");
		String expected = TestFileUtils.getTestFile("/StreamLineIteratorPipe/EndMarkedResult.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());
		
		assertXmlEquals(expected, actual);
	}

	@Test
	public void testStartOfLineString() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setStartOfLineString("BOL");
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/StreamLineIteratorPipe/BeginMarked.txt");
		String expected = TestFileUtils.getTestFile("/StreamLineIteratorPipe/BeginMarkedResult.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertXmlEquals(expected, actual);
		
	}
}
