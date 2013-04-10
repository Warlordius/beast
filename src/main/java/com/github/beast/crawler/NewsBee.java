package com.github.beast.crawler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import com.github.beast.Beast;
import com.github.beast.page.ArticlePage;
import com.github.beast.page.Page;
import com.github.beast.util.Configuration;

/**
 * NewsBee extends the {@link Bee} class, representing a specialized agent for
 * tracking of news stories from news articles on the Web. In addition to
 * carrying {@link Bee#source}, every bee also carries a specific
 * {@link #keyword}. Instead of rating only the content of a {@link Page Pages},
 * keywords are used in the calculation of {@link Bee#quality} as well.
 * 
 * @author Štefan Sabo
 * @see Bee
 */
public class NewsBee extends Bee {

	private double newQuality;
	private ArticlePage newSource;
	private String keyword;

	public NewsBee(Crawler crawler) {

		super(crawler);
		
		this.keyword = crawler.index.getRandKeyword(source);

		if (Configuration.getInstance().useBeeMessages()) {
			System.out.println("Init: " + source.getTitle() + " --- " + keyword);
		}
	}

	public double evalQuality(final Object args) {

		return 0;
	}

	/**
	 * Evaluate the relevance of a given page to a given keyword, considering
	 * both content of the page and currently identified keywords. If the given
	 * keyword was already identified and its relevance value is higher than
	 * relevance value obtained only from the page content, old keyword
	 * relevance is used, otherwise value based on the page content is returned.
	 * 
	 * @param page page to be evaluated.
	 * @param keyword keyword to be considered.
	 * @return A double representing the relevance of the given page to the
	 *         given keyword.
	 */
	protected double evalQuality(final ArticlePage page, final String keyword) {

		// evaluate the quality of keyword considering only the page
		double pageQuality = evalSourceQuality(page, keyword);

		// evaluate the quality of keyword considering page surround
		double surroundQuality = evalSurroundQuality(page, keyword);

		return Math.max(pageQuality, surroundQuality);
	}

	/**
	 * Evaluate the relevance of a given page to a given keyword, considering
	 * only the content of the page. Uses semantics if enabled.
	 * 
	 * @param page - Page to be evaluated.
	 * @param keyword - Keyword to be used for evaluation.
	 * @return A double representing the relevance of the given page to the
	 *         given keyword.
	 */
	protected double evalSourceQuality(final ArticlePage page, final String keyword) {

		final int granularity = 1000;

		int maxPoints = 0;
		int recPoints = 0;

		ArrayList<String> lookups;

		if (Configuration.getInstance().useSemantics()) {

			lookups = Beast.semEngine.getSynonyms(keyword);

			for (int i = 0; i < lookups.size(); i++) {
				lookups.set(i, lookups.get(i).toLowerCase());
			}
		} else {
			lookups = new ArrayList<String>();
			lookups.add(keyword.toLowerCase());
		}

		String keywordInLow = keyword.toLowerCase();

		if (!page.isIndexed()) {
			Beast.crawler.index.indexPage(page);
		}

		maxPoints = (int) Math.ceil(page.getText().length() / granularity);

		if (maxPoints > 0) {
			// careful not to exceed the end of string
			for (int i = 0; i < maxPoints - 1; i++) {
				String subString = page.getText().substring(i * granularity, (i + 1) * granularity);
				if (subString.toLowerCase().contains(keywordInLow)) {
					recPoints = recPoints + 1;
				}
			}

			// final part of string that contains <= GRANULARITY chars
			String subString = page.getText().substring((maxPoints - 1) * granularity);

			if (subString.toLowerCase().contains(keywordInLow)) {
				recPoints = recPoints + 1;
			}

			// increase for perex
			if ((page.getPerex() != null) && (page.getPerex().length() > 0)) {
				maxPoints = maxPoints + 2;
				if (page.getPerex().toString().toLowerCase().contains(keywordInLow)) {
					recPoints = recPoints + 2;
				}
			}
		}

		// increase for title
		if ((page.getTitle() != null) && (!page.getTitle().isEmpty())) {
			maxPoints = maxPoints + 5;
			if (page.getTitle().toLowerCase().contains(keywordInLow)) {
				recPoints = recPoints + 5;
			}
		}

		if (recPoints > maxPoints) {
			recPoints = maxPoints;
		}

		// evalute the quality of keyword considering only the page
		double pageQuality = (double) recPoints / maxPoints;

		return pageQuality;
	}

	/**
	 * Evaluate the relevance of a given page to a given keyword, considering
	 * the pre-existing keywords relevance, disregarding the page content
	 * itself.
	 * 
	 * @param page - Page to be evaluated.
	 * @param keyword - Keyword to be used for evaluation.
	 * @return A double representing the relevance of the given page to the
	 *         given keyword.
	 */
	protected double evalSurroundQuality(final ArticlePage page, final String keyword) {

		Double relevance = crawler.index.getKeywordRelevance(keyword, page);
		return relevance;
	}

	/**
	 * Method encapsulating foraging behavior of a bee. The method is invoked,
	 * if a bee decides to forage for its source. During foraging, current
	 * source is processed, if not processed before and the quality of source is
	 * assessed, considering the current keyword.
	 * 
	 * Subsequently a new source is selected and probed, so that further
	 * decisions may be undertaken in the future.
	 */
	protected void doWhileForaging() {

		Calendar now = Calendar.getInstance();
		long timeSinceRefresh = now.getTimeInMillis() - source.getLastIndexed().getTime();
		ArticlePage sourceArticle = (ArticlePage) source;
		
		if (Configuration.getInstance().useBeeMessages()) {
			System.out.println("Foraging: " + source.getUrl().toString() + ", keyword: " + keyword);
		}

		// process the source first, if not processed yet. if processing
		// fails, leave immediatelly
		if (!source.isProcessed()) {

			try {
				source.process();
			} catch (NullPointerException e) {
				System.err.println("Failed to process page: " + source.getUrl());
				desire = 0;
				return;
			}
			Beast.log("new source found: " + sourceArticle.getTimestamp().toString() + " " + source.getUrl().toString());
		}
		quality = evalQuality(sourceArticle, keyword);

		// visit a neighbouring source
		newSource = (ArticlePage) crawler.index.getRandNeighbour(source);
		if (newSource == null) {
			System.err.println("Failed to process page: " + source.getUrl());
			desire = 0;
			return;
		}

		// if source is sucessfully processed, index it, otherwise leave
		if (!newSource.isIndexed()) {
			try {
				newSource.process();
				crawler.index.indexPage(newSource);
			} catch (NullPointerException e) {
				desire = 0;
				return;
			}
		}

		// if source is old enough, reindex it anew
		if ((source.isIndexed()) && (timeSinceRefresh > Configuration.getInstance().getRefreshDelay())) {
			crawler.index.reindexPage(sourceArticle);
			quality = evalQuality(sourceArticle, keyword);
			Beast.log("source refreshed: " + sourceArticle.getTimestamp().toString() + " " + quality + " " + source.getUrl().toString());
		}

		newQuality = evalQuality(newSource, keyword);
		desire = Math.min(quality, newQuality);
	}

	protected void doWhileObserving() {

		if ((Configuration.getInstance().useBeeMessages()) && (source != null)) {
			System.out.println("Observing: " + source.getUrl().toString());
		}

		source = null;
	}

	public boolean decideToDance() {

		final double reducingFactor = 1;

		Random generator = new Random();

		if (generator.nextDouble() > (desire * reducingFactor)) {
			return false;
		}
		return true;
	}

	protected void doWhileDancing() {

		if (Configuration.getInstance().useBeeMessages()) {
			System.out.println("Dancing: " + source.getUrl().toString() + " keyword: " + keyword);
		}

		if (firstDance) {
			crawler.index.addRelation(source, newSource, keyword, desire);
			crawler.index.addKeyword(source, keyword, Math.max(quality, newQuality * crawler.DECAY));
			crawler.index.addKeyword(newSource, keyword, Math.max(quality * crawler.DECAY, newQuality));
			firstDance = false;
		}
	}

	protected void follow(NewsBee bee) {

		if (bee.status == Status.DANCING) {
			this.status = Status.FORAGING;
			this.source = bee.source;
			this.keyword = crawler.index.getRandKeyword(source);
		}
	}

	public void dispatch() {

		source = (ArticlePage) crawler.randomSource();
		keyword = crawler.index.getRandKeyword(source);
		status = Status.FORAGING;
	}
}