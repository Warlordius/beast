package com.github.beast.crawler;

import java.util.Random;

import com.github.beast.page.Page;

/**
 * Abstract class representing a single Bee - agent inspired by social insect.
 * 
 * The agent visits one web {@link Page} at a time, stored in {@link #source}
 * field and decides, whether to propagate the source for other agents, continue
 * visiting the source further, or abandon the source and select a different
 * one.
 * <p>
 * The decision are based on the rating of a source, calculated in abstract
 * method {@link #evalQuality()}. Specific conditions of electing to propagate
 * or abandon source may be specified by overriding {@link #decideToDance()} and
 * {@link #decideToLeave()} methods respectively.
 * <p>
 * All bees are assigned to a {@link Crawler} at construction. Individual agents
 * act independently, however the crawler holds information about all agents'
 * actions.
 * 
 * @version 1.0
 * @author Štefan Sabo
 * 
 * @see Crawler
 * @see Page
 */
public abstract class Bee extends Thread {

	/**
	 * An abstract class representing a target for processing by {@link Bee
	 * Bees}.
	 * 
	 * @author Štefan Sabo
	 */
	protected abstract class Source {
		protected int num;
		protected int numSecond;
	}

	/**
	 * Possible statuses of a bee, <i>foraging</i> {@link Bee} is currently
	 * processing a {@link Source}, <i>observing</i> Bee holds no Source and is
	 * deciding for a new one to process and <i>dancing</i> bee is currently
	 * propagating its Source, therefore recruiting other Bees to forage from
	 * the same Source.
	 */
	protected enum Status {
		FORAGING, OBSERVING, DANCING;
	}

	protected boolean firstDance = false;	// true when starting to dance
	protected int timeToDance;
	protected int timeToObserve;
	protected Status status;

	/** Rating of the visited {@link #source}. */
	protected double quality;

	/** Adjusted {@link #quality} used as base for decisions of {@link Bee}. */
	protected double desire;

	/** A web page currently being visited by the {@link Bee}. */
	protected Page source;

	/** A {@link Crawler} to which the {@link Bee} is assigned. */
	protected Crawler crawler;

	/**
	 * Constructor for the Bee class, takes a single mandatory parameter of
	 * {@link Crawler}, to which the created Bee will be assigned. During the
	 * construction a random source is assigned to the Bee and its status is set
	 * to foraging.
	 * 
	 * @param parentCrawler the {@link Crawler} to which the bee will be
	 *        assigned
	 * @see Crawler
	 */
	public Bee(final Crawler parentCrawler) {

		crawler = parentCrawler;
		source = parentCrawler.randomSource();
		status = Status.FORAGING;
	}

	/**
	 * Performs a single iteration of the bee. Method encapsulates the decision
	 * tree of a bee, deciding on whether to dance, forage, or observe in the
	 * next iteration.
	 */
	protected final void doIteration() {

		if (status == Status.FORAGING) {

			forage();
			if (decideToLeave()) {
				timeToObserve = getTimeToObserve();
				status = Status.OBSERVING;
			} else if (decideToDance()) {
				timeToDance = getTimeToDance();
				firstDance = true;
				status = Status.DANCING;
			}
			return;

		} else if (status == Status.OBSERVING) {
			observe();
			return;
		} else if (status == Status.DANCING) {
			dance();
		}
	}

	/**
	 * Method encapsulating foraging behavior of a bee. Default behavior is
	 * staying idle, different behavior may be specified by overriding this
	 * method.
	 */
	protected void forage() {

		doWhileForaging();
	}

	/**
	 * Method encapsulating observing behavior of a bee. First a bee to be
	 * followed is selected. If this bee is dancing, it is followed. If the bee
	 * is not dancing, nothing happens in this round, except decreasing the time
	 * to observe.
	 * 
	 * If the time to observe runs out, the bee is dispatched, thus ending the
	 * observation phase.
	 */
	protected void observe() {

		Random generator = new Random();
		int randomBeeNumber = generator.nextInt(crawler.bees.size());
		Bee randomBee = crawler.bees.get(randomBeeNumber);

		timeToObserve--;
		doWhileObserving();

		if (randomBee.status == Status.DANCING) {
			follow(randomBee);
		}
		if (timeToObserve <= 0) {
			dispatch();
		}
	}

	/**
	 * Method encapsulating dancing behavior of a bee. The dance continues for a
	 * set number of rounds, after which the bee returns to foraging.
	 */
	protected void dance() {

		timeToDance--;
		doWhileDancing();

		if (timeToDance <= 0) {
			status = Status.FORAGING;
		}
	}

	/**
	 * Method called, when a bee fails to select a new source during observation
	 * phase and is dispatched to forage from a random source.
	 */
	protected void dispatch() {

		source = crawler.randomSource();
		status = Status.FORAGING;
	}

	/**
	 * Is invoked when a bee decides to follow a different bee during
	 * observation phase, effectively adopting a new source and starting to
	 * forage from it. Default behavior is to follow only dancing bees.
	 * 
	 * @param bee the bee to be followed
	 */
	protected void follow(final Bee bee) {

		if (bee.status == Status.DANCING) {
			this.source = bee.source;
			this.status = Status.FORAGING;
		}
	}

	/**
	 * Represents a single random decision of bee whether to dance for the
	 * current source, based on current desire value of a bee. Under default
	 * behavior, the probability of propagating (dancing for) a source is
	 * directly proportional to the current {@link #desire}.
	 * 
	 * @return true if bee wants to dance for its source, otherwise false
	 */
	protected boolean decideToDance() {

		Random generator = new Random();
		if (generator.nextDouble() > desire) {
			return false;
		}
		return true;
	}

	/**
	 * Represents a single random decision of bee whether to abandon the current
	 * source, based on current desire value of a bee. Under default behavior,
	 * the probability of abandoning a source is directly proportional to <i>1 -
	 * {@link #desire}</i>.
	 * 
	 * @return true if bee wants to abandon its source, otherwise false
	 */
	protected boolean decideToLeave() {

		Random generator = new Random();
		if (generator.nextDouble() > desire) {
			return true;
		}
		return false;
	}

	/**
	 * Evaluates the quality of currently visited source. Needs to be
	 * implemented in derived class. The number and type of parameters may
	 * differ for implementing methods, therefore parameter object is used to
	 * hold arguments.
	 * 
	 * @param args parameter object passed to method
	 * @return quality of currently visited source as a value from the interval
	 *         of <code><0;1></code>
	 */
	protected abstract double evalQuality(Object args);

	/**
	 * Evaluates the desire of bee to dance / retain the currently visited
	 * source, based on the current {@link #quality} of a source. Defaults to
	 * returning original value of quality, may however incorporate additional
	 * dynamic factors, such as number of bees on a given source at the time of
	 * calculation, etc.
	 * 
	 * @param currentQuality quality of the current source, without adjustments
	 * @return value from the interval of <code><0;1></code>
	 */
	protected double evalDesire(final double currentQuality) {

		double newDesire;

		// TODO: rewrite to implement counters of bees at sources, without
		// iterating over bees

		// desire disabled
		// desire = quality - ( crawler.beesAtSource(source) *
		// crawler.DESIRE_REDUCTION );
		newDesire = currentQuality;

		return (newDesire > 0) ? newDesire : 0;
	}

	/**
	 * Calculate the initial length of a propagation for a given source.
	 * 
	 * @return the length of propagation in rounds
	 * @see #getTimeToObserve()
	 */
	protected int getTimeToDance() {

		int time = (int) Math.ceil(crawler.MAX_DANCE_TIME * quality);
		return time;
	}

	/**
	 * Calculate the initial length of observation.
	 * 
	 * @return the length of observation in rounds
	 * @see #getTimeToDance()
	 */
	protected int getTimeToObserve() {

		return crawler.MAX_OBSERVE_TIME;
	}

	/**
	 * Method encapsulating additional actions of a bee to be performed during
	 * dancing phase.
	 */
	protected void doWhileDancing() {

	}

	/**
	 * Method encapsulating additional actions of a bee to be performed during
	 * foraging phase.
	 */
	protected void doWhileForaging() {

	}

	/**
	 * Method encapsulating additional actions of a bee to be performed during
	 * observation phase.
	 */
	protected void doWhileObserving() {

	}
}