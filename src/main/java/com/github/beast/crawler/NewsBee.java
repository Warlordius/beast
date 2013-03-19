package com.github.beast.crawler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import com.github.beast.Beast;
import com.github.beast.page.Page;

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
	private Page newSource;
	private String keyword;

	public NewsBee(Crawler crawler) {

		super(crawler);
		this.keyword = crawler.index.getRandKeyword(source);

		if (Beast.config.useBeeMessages()) {
			System.out.println("Init: " + source.title + " --- " + keyword);
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
	protected double evalQuality(final Page page, final String keyword) {

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
	protected double evalSourceQuality(final Page page, final String keyword) {

		final int GRANULARITY = 1000;

		int maxPoints = 0;
		int recPoints = 0;

		ArrayList<String> lookups;
		if (Beast.config.useSemantics()) {

			lookups = Beast.semEngine.getSynonyms(keyword);

			for (int i = 0; i < lookups.size(); i++) {
				lookups.set(i, lookups.get(i).toLowerCase());
			}
		}

		else {
			lookups = new ArrayList<String>();
			lookups.add(keyword.toLowerCase());
		}

		String keywordInLow = keyword.toLowerCase();

		if (!page.isIndexed()) {
			Beast.crawler.index.indexPage(page);
		}

		maxPoints = (int) Math.ceil(page.text.length() / GRANULARITY);

		if (maxPoints > 0) {
			// careful not to exceed the end of string
			for (int i = 0; i < maxPoints - 1; i++) {
				String subString = page.text.substring(i * GRANULARITY, (i + 1) * GRANULARITY);
				if (subString.toLowerCase().contains(keywordInLow)) {
					recPoints = recPoints + 1;
				}
			}

			// final part of string that contains <= GRANULARITY chars
			String subString = page.text.substring((maxPoints - 1) * GRANULARITY);

			if (subString.toLowerCase().contains(keywordInLow)) {
				recPoints = recPoints + 1;
			}

			// increase for perex
			if ((page.perex != null) && (page.perex.length() > 0)) {
				maxPoints = maxPoints + 2;
				if (page.perex.toString().toLowerCase().contains(keywordInLow)) {
					recPoints = recPoints + 2;
				}
			}
		}

		// increase for title
		if ((page.title != null) && (!page.title.isEmpty())) {
			maxPoints = maxPoints + 5;
			if (page.title.toLowerCase().contains(keywordInLow)) {
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
	protected double evalSurroundQuality(final Page page, final String keyword) {

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
		long timeSinceRefresh = now.getTimeInMillis() - source.lastIndexed.getTime();

		if (Beast.config.useBeeMessages()) {
			System.out.println("Foraging: " + source.url.toString() + ", keyword: " + keyword);
		}

		// process the source first, if not processed yet. if processing
		// fails, leave immediatelly
		if (!source.isProcessed()) {

			if (!source.process()) {
				// desire = 0;
				return;
			}

			Beast.log("new source found: " + source.timestamp.toString() + " " + source.url.toString());
		}
		quality = evalQuality(source, keyword);

		// visit a neighbouring source
		newSource = crawler.index.getRandNeighbour(source);
		if (newSource == null) {
			desire = 0;
			return;
		}

		// if source is sucessfully processed, index it, otherwise leave
		if (!newSource.isIndexed()) {
			if (newSource.process()) {
				crawler.index.indexPage(newSource);
			} else {
				desire = 0;
				return;
			}
		}

		// if source is old enough, reindex it anew
		if ((source.isIndexed()) && (timeSinceRefresh > Beast.config.getRefreshDelay())) {
			crawler.index.reindexPage(source);
			quality = evalQuality(source, keyword);
			Beast.log("source refreshed: " + source.timestamp.toString() + " " + quality + " " + source.url.toString());
		}

		newQuality = evalQuality(newSource, keyword);
		desire = Math.min(quality, newQuality);
	}

	protected void doWhileObserving() {

		if ((Beast.config.useBeeMessages()) && (source != null)) {
			System.out.println("Observing: " + source.url.toString());
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

		if (Beast.config.useBeeMessages()) {
			System.out.println("Dancing: " + source.url.toString() + " keyword: " + keyword);
		}

		if (firstDance) {
			crawler.index.addRelation(source, newSource, keyword, desire);
			crawler.index.addKeyword(source, keyword, Math.max(quality, newQuality * crawler.DECAY));
			crawler.index.addKeyword(newSource, keyword, Math.max(quality * crawler.DECAY, newQuality));
			firstDance = false;
		}
	}

	protected void follow(Bee bee) {

		if (bee.status == Status.DANCING) {
			this.status = Status.FORAGING;
			this.source = bee.source;
			this.keyword = crawler.index.getRandKeyword(source);
		}
	}

	public void dispatch() {

		source = crawler.randomSource();
		keyword = crawler.index.getRandKeyword(source);
		status = Status.FORAGING;
	}
}