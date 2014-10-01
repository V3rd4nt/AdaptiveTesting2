package at.reisisoft.jku.ce.adaptivelearning.vaadin.ui.topic.accounting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import at.reisisoft.jku.ce.adaptivelearning.VaadinUI;
import at.reisisoft.jku.ce.adaptivelearning.topic.accounting.AccountingDataProvider;
import at.reisisoft.jku.ce.adaptivelearning.topic.accounting.AccountingDataStorage;
import at.reisisoft.jku.ce.adaptivelearning.topic.accounting.AccountingQuestion;
import at.reisisoft.jku.ce.adaptivelearning.topic.accounting.ProfitDataStorage;
import at.reisisoft.jku.ce.adaptivelearning.topic.accounting.ProfitQuestion;
import at.reisisoft.jku.ce.adaptivelearning.vaadin.ui.QuestionManager;
import at.reisisoft.jku.ce.adaptivelearning.xml.topic.accounting.AccountingXmlHelper;
import at.reisisoft.jku.ce.adaptivelearning.xml.topic.accounting.XmlAccountingQuestion;
import at.reisisoft.jku.ce.adaptivelearning.xml.topic.accounting.XmlProfitQuestion;

import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Window;

public class AccountingQuestionManager extends QuestionManager {

	private static final long serialVersionUID = -4764723794449575244L;

	public AccountingQuestionManager(String quizName) {
		super(quizName);
		Button openKontenplan = new Button("Open Kontenplan");
		openKontenplan.addClickListener(e -> {
			openKontenplan.setEnabled(false);
			// Create Window with layout
				Window window = new Window("Kontenplan");
				GridLayout layout = new GridLayout(1, 1);
				layout.addComponent(AccountingDataProvider.getInstance()
						.toHtmlTable());
				layout.setSizeFull();
				window.setContent(layout);
				window.center();
				window.setWidth("60%");
				window.setHeight("80%");
				window.setResizable(false);
				window.addCloseListener(e1 -> openKontenplan.setEnabled(true));
				getUI().addWindow(window);

			});
		addHelpButton(openKontenplan);
		try {
			loadQuestions(new File(VaadinUI.Servlet.getQuestionFolderName()));
		} catch (JAXBException | IOException e1) {
			Notification.show("Questions could not be loaded - FATAL error",
					e1.getMessage() + Arrays.toString(e1.getStackTrace()),
					Type.ERROR_MESSAGE);
		}

	}

	public int loadQuestions(File containingFolder) throws JAXBException,
	IOException {
		assert containingFolder.exists() && containingFolder.isDirectory();
		JAXBContext accountingJAXB = JAXBContext.newInstance(
				XmlAccountingQuestion.class, AccountingDataStorage.class);
		JAXBContext profitJAXB = JAXBContext.newInstance(
				XmlProfitQuestion.class, ProfitDataStorage.class);

		Unmarshaller accountingUnmarshaller = accountingJAXB
				.createUnmarshaller();
		Unmarshaller profitUnmarshaller = profitJAXB.createUnmarshaller();

		final List<AccountingQuestion> accountingList = new ArrayList<>();
		final List<ProfitQuestion> profitList = new ArrayList<>();

		String accountingRootElement = XmlAccountingQuestion.class
				.getAnnotation(XmlRootElement.class).name();
		String profitRootElement = XmlProfitQuestion.class.getAnnotation(
				XmlRootElement.class).name();

		File[] questions = containingFolder.listFiles((FileFilter) f -> f
				.isFile()
				&& (f.canRead() || f.setReadable(true))
				&& f.getName().endsWith(".xml"));

		// read all questions
		for (File f : questions) {
			BufferedReader reader = null;
			StringBuilder sb = new StringBuilder();
			try {
				reader = new BufferedReader(new InputStreamReader(
						new BOMInputStream(new FileInputStream(f),
								ByteOrderMark.UTF_8)));

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
			String fileAsString = sb.toString();
			if (fileAsString.contains(profitRootElement)) {
				// Profit Question
				XmlProfitQuestion question = (XmlProfitQuestion) profitUnmarshaller
						.unmarshal(new StringReader(fileAsString));
				profitList.add(AccountingXmlHelper.fromXml(question));
			} else if (fileAsString.contains(accountingRootElement)) {
				// Accounting Question
				XmlAccountingQuestion question = (XmlAccountingQuestion) accountingUnmarshaller
						.unmarshal(new StringReader(fileAsString));
				accountingList.add(AccountingXmlHelper.fromXml(question));
			} else {
				throw new IllegalArgumentException(
						"Question type not supported. File: " + f);
			}

		}
		// Add question to the question manager
		accountingList.forEach(q -> addQuestion(q));
		profitList.forEach(q -> addQuestion(q));
		return questions.length;
	}
}
