/**
 * 
 */
package nl.nn.adapterframework.testtool.controller;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.testtool.*;

import java.util.*;

/**
 * This class is used to initialize and execute File Senders and Listeners.
 * @author Jaco de Groot, Murat Kaan Meral
 *
 */
public class FileController {

	MessageListener messageListener;
	ScenarioTester scenarioTester;
	ResultComparer resultComparer;

	public FileController(ScenarioTester scenarioTester) {
		this.scenarioTester = scenarioTester;
		this.messageListener = scenarioTester.getMessageListener();
		this.resultComparer = new ResultComparer(messageListener);
	}

	/**
	 * Initializes the senders specified by fileSenders and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param fileSenders List of file senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void initSender(Map<String, Map<String, Object>> queues, List<String> fileSenders, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		messageListener.debugMessage(testName, "Initialize file senders");
		Iterator<String> iterator = fileSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String filename  = (String)properties.get(queueName + ".filename");
			if (filename == null) {
				scenarioTester.closeQueues(queues, properties);
				queues = null;
				messageListener.errorMessage(testName, "Could not find filename property for " + queueName);
			} else {
				FileSender fileSender = new FileSender();
				String filenameAbsolutePath = (String)properties.get(queueName + ".filename.absolutepath");
				fileSender.setFilename(filenameAbsolutePath);
				String encoding = (String)properties.get(queueName + ".encoding");
				if (encoding != null) {
					fileSender.setEncoding(encoding);
					messageListener.debugMessage(testName, "Encoding set to '" + encoding + "'");
				}
				String deletePathString = (String)properties.get(queueName + ".deletePath");
				if (deletePathString != null) {
					boolean deletePath = Boolean.valueOf(deletePathString).booleanValue();
					fileSender.setDeletePath(deletePath);
					messageListener.debugMessage(testName, "Delete path set to '" + deletePath + "'");
				}
				String createPathString = (String)properties.get(queueName + ".createPath");
				if (createPathString != null) {
					boolean createPath = Boolean.valueOf(createPathString).booleanValue();
					fileSender.setCreatePath(createPath);
					messageListener.debugMessage(testName, "Create path set to '" + createPath + "'");
				}
				try {
					String checkDeleteString = (String)properties.get(queueName + ".checkDelete");
					if (checkDeleteString != null) {
						boolean checkDelete = Boolean.valueOf(checkDeleteString).booleanValue();
						fileSender.setCheckDelete(checkDelete);
						messageListener.debugMessage(testName, "Check delete set to '" + checkDelete + "'");
					}
				} catch(Exception e) {
				}
				try {
					String runAntString = (String)properties.get(queueName + ".runAnt");
					if (runAntString != null) {
						boolean runAnt = Boolean.valueOf(runAntString).booleanValue();
						fileSender.setRunAnt(runAnt);
						messageListener.debugMessage(testName, "Run ant set to '" + runAnt + "'");
					}
				} catch(Exception e) {
				}
				try {
					long timeOut = Long.parseLong((String)properties.get(queueName + ".timeOut"));
					fileSender.setTimeOut(timeOut);
					messageListener.debugMessage(testName, "Time out set to '" + timeOut + "'");
				} catch(Exception e) {
				}
				try {
					long interval  = Long.parseLong((String)properties.get(queueName + ".interval"));
					fileSender.setInterval(interval);
					messageListener.debugMessage(testName, "Interval set to '" + interval + "'");
				} catch(Exception e) {
				}
				try {
					String overwriteString = (String)properties.get(queueName + ".overwrite");
					if (overwriteString != null) {
						messageListener.debugMessage(testName, "OverwriteString = " + overwriteString);
						boolean overwrite = Boolean.valueOf(overwriteString).booleanValue();
						fileSender.setOverwrite(overwrite);
						messageListener.debugMessage(testName, "Overwrite set to '" + overwrite + "'");
					}
				} catch(Exception e) {
				}
				Map<String, Object> fileSenderInfo = new HashMap<String, Object>();
				fileSenderInfo.put("fileSender", fileSender);
				queues.put(queueName, fileSenderInfo);
				messageListener.debugMessage(testName, "Opened file sender '" + queueName + "'");
			}
		}

	}

	/**
	 * Initializes the listeners specified by fileListeners and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param fileListeners List of file listeners to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void initListener(Map<String, Map<String, Object>> queues, List<String> fileListeners, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		messageListener.debugMessage(testName, "Initialize file listeners");
		Iterator<String> iterator = fileListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String filename  = (String)properties.get(queueName + ".filename");
			String filename2  = (String)properties.get(queueName + ".filename2");
			String directory = null;
			String wildcard = null;
			if (filename == null) {
				directory = (String)properties.get(queueName + ".directory");
				wildcard = (String)properties.get(queueName + ".wildcard");
			}
			if (filename == null && directory == null) {
				scenarioTester.closeQueues(queues, properties);
				queues = null;
				messageListener.errorMessage(testName, "Could not find filename or directory property for " + queueName);
			} else if (directory != null && wildcard == null) {
				scenarioTester.closeQueues(queues, properties);
				queues = null;
				messageListener.errorMessage(testName, "Could not find wildcard property for " + queueName);
			} else {
				FileListener fileListener = new FileListener();
				if (filename == null) {
					String directoryAbsolutePath = (String)properties.get(queueName + ".directory.absolutepath");;
					fileListener.setDirectory(directoryAbsolutePath);
					fileListener.setWildcard(wildcard);
				} else {
					String filenameAbsolutePath = (String)properties.get(queueName + ".filename.absolutepath");;
					fileListener.setFilename(filenameAbsolutePath);
				}
				try {
					long waitBeforeRead = Long.parseLong((String)properties.get(queueName + ".waitBeforeRead"));
					fileListener.setWaitBeforeRead(waitBeforeRead);
					messageListener.debugMessage(testName, "Wait before read set to '" + waitBeforeRead + "'");
				} catch(Exception e) {
				}
				try {
					long timeOut = Long.parseLong((String)properties.get(queueName + ".timeOut"));
					fileListener.setTimeOut(timeOut);
					messageListener.debugMessage(testName, "Time out set to '" + timeOut + "'");
				} catch(Exception e) {
				}
				try {
					long interval  = Long.parseLong((String)properties.get(queueName + ".interval"));
					fileListener.setInterval(interval);
					messageListener.debugMessage(testName, "Interval set to '" + interval + "'");
				} catch(Exception e) {
				}
				if (filename2!=null) {
					fileListener.setFilename2(filename2);
				}
				Map<String, Object> fileListenerInfo = new HashMap<String, Object>();
				fileListenerInfo.put("fileListener", fileListener);
				queues.put(queueName, fileListenerInfo);
				messageListener.debugMessage(testName, "Opened file listener '" + queueName + "'");
				if (fileListenerCleanUp(queueName, fileListener, testName)) {
					messageListener.errorMessage(testName, "Found old messages on '" + queueName + "'");
				}
			}
		}
	}

	/**
	 * Closes the file listeners that are in the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void closeListener(Map<String, Map<String, Object>> queues, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		messageListener.debugMessage(testName, "Close file listeners");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.testtool.FileListener".equals(properties.get(queueName + ".className"))) {
				FileListener fileListener = (FileListener)((Map<?, ?>)queues.get(queueName)).get("fileListener");
				fileListenerCleanUp(queueName, fileListener, testName);
				messageListener.debugMessage(testName, "Closed file listener '" + queueName + "'");
			}
		}
	}
	
	/**
	 * Checks if there are any remaining messages for file listener.
	 * @param queueName name of the pipe to be used.
	 * @param fileListener listener that receives the message.
	 * @return true if there are any unexpected remaining messages.
	 */
	private boolean fileListenerCleanUp(String queueName, FileListener fileListener, String testName) {
		boolean remainingMessagesFound = false;
		messageListener.debugMessage(testName, "Check for remaining messages on '" + queueName + "'");
		if (fileListener.getFilename2()!=null) {
			return false;
		}
		long oldTimeOut = fileListener.getTimeOut();
		fileListener.setTimeOut(0);
		boolean empty = false;
		fileListener.setTimeOut(0);
		try {
			String message = fileListener.getMessage();
			if (message != null) {
				remainingMessagesFound = true;
				messageListener.wrongPipelineMessage(testName, "Found remaining message on '" + queueName + "'", message);
			}
		} catch(TimeOutException e) {
		} catch(ListenerException e) {
			messageListener.errorMessage(testName, "Could read message from file listener '" + queueName + "': " + e.getMessage(), e);
		}
		fileListener.setTimeOut(oldTimeOut);
		return remainingMessagesFound;
	}

	/**
	 * Sends the given string to the fileSender.
	 * @param stepDisplayName to be displayed, used for debugging.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileContent message to write on the fileSender pipe.
	 * @return 1 if everything is ok, 0 if there has been an error.
	 */
	public int executeSenderWrite(String testName, String stepDisplayName, Map<String, Map<String, Object>> queues, String queueName, String fileContent) {
		int result = TestTool.RESULT_ERROR;
		Map<?, ?> fileSenderInfo = (Map<?, ?>)queues.get(queueName);
		FileSender fileSender = (FileSender)fileSenderInfo.get("fileSender");
		try {
			fileSender.sendMessage(fileContent);
			messageListener.debugPipelineMessage(testName, stepDisplayName, "Successfully written to '" + queueName + "':", fileContent);
			result = TestTool.RESULT_OK;
		} catch(Exception e) {
			messageListener.errorMessage(testName, "Exception writing to file: " + e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Reads the output of the pipe and compares it to the expected result.
	 * @param step string that contains the whole step.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileName name of the file that contains the expected result.
	 * @param fileContent Content of the file that contains expected result.
	 * @return 1 if everything is ok, 0 if there has been an error.
	 */
	public int executeListenerRead(String testName, String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, String queueName, String fileName, String fileContent, String  originalFilePath) {
		int result = TestTool.RESULT_ERROR;
		Map<?, ?> fileListenerInfo = (Map<?, ?>)queues.get(queueName);
		FileListener fileListener = (FileListener)fileListenerInfo.get("fileListener");
		String message = null;
		try {
			message = fileListener.getMessage();
		} catch(Exception e) {
			if (!"".equals(fileName)) {
				messageListener.errorMessage(testName, "Could not read file from '" + queueName + "': " + e.getMessage(), e);
			}
		}
		if (message == null) {
			if ("".equals(fileName)) {
				result = TestTool.RESULT_OK;
			} else {
				messageListener.errorMessage(testName, "Could not read file (null returned)");
			}
		} else {
			result = resultComparer.compareResult(step, stepDisplayName, fileName, fileContent, message, properties, queueName, originalFilePath);
		}
		return result;	
	}

	/**
	 * Reads the output of the pipe and compares it to the expected result.
	 * @param step string that contains the whole step.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileName name of the file that contains the expected result.
	 * @param fileContent Content of the file that contains expected result.
	 * @return 1 if everything is ok, 0 if there has been an error.
	 */
	public int executeSenderRead(String testName, String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, String queueName, String fileName, String fileContent, String originalFilePath) {
		int result = TestTool.RESULT_ERROR;
		Map<?, ?> fileSenderInfo = (Map<?, ?>)queues.get(queueName);
		FileSender fileSender = (FileSender)fileSenderInfo.get("fileSender");
		String message = null;
		try {
			message = fileSender.getMessage();
		} catch(Exception e) {
			if (!"".equals(fileName)) {
				messageListener.errorMessage(testName, "Could not read file from '" + queueName + "': " + e.getMessage(), e);
			}
		}
		if (message == null) {
			if ("".equals(fileName)) {
				result = TestTool.RESULT_OK;
			} else {
				messageListener.errorMessage(testName, "Could not read file (null returned)");
			}
		} else {
			result = resultComparer.compareResult(step, stepDisplayName, fileName, fileContent, message, properties, queueName, originalFilePath);
		}
		return result;	
	}
}