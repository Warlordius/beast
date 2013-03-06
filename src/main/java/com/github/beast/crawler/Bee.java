package com.github.beast.crawler;

import java.util.*;

import com.github.beast.page.Page;

/**
 * An abstract class representing a single Bee - agent inspired by social
 * insect. The agent visits one web {@link Page} at a time, stored in
 * {@link Bee#source} field and decides, whether to propagate the source for
 * other agents, continue visiting the source further, or abandon the source and
 * select a different one.
 * <p>
 * The decision are based on the rating of a source, calculated in abstract
 * method {@link Bee#evalQuality()}. Specific conditions of electing to
 * propagate or abandon source may be specified by overriding
 * {@link Bee#decideToDance()} and {@link Bee#decideToLeave()} methods
 * respectively.
 * <p>
 * All bees are assigned to a {@link Crawler} at construction. Individual agents
 * act independently, however the crawler holds information about all agents'
 * actions.
 * 
 * @author Štefan Sabo
 * @see Crawler
 * @see Page
 */
public abstract class Bee extends Thread {

    protected static final int FORAGING = 0;
    protected static final int OBSERVING = 1;
    protected static final int DANCING = 2;

    protected boolean firstDance = false;	// set to true whenever starting to dance 
    protected int status;			// TODO: change to enum
    protected int timeToDance;
    protected int timeToObserve;

    /** Rating of the visited {@link Bee#source} */
    protected double quality;

    /** Adjusted {@link Bee#quality} used as base for decisions of {@link Bee}. */
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
     * @param crawler the {@link Crawler} to which the bee will be assigned
     * @see Crawler
     */
    public Bee(Crawler crawler) {

	this.crawler = crawler;
	this.source = crawler.randomSource();
	this.status = FORAGING;
    }

    /**
     * Performs a single iteration of the bee. Method encapsulates the decision
     * tree of a bee, deciding on whether to dance, forage, or observe in the
     * next iteration.
     */
    final protected void doIteration() {

	if (status == FORAGING) {

	    forage();
	    if (decideToLeave()) {
		timeToObserve = getTimeToObserve();
		status = OBSERVING;
	    } else if (decideToDance()) {
		timeToDance = getTimeToDance();
		firstDance = true;
		status = DANCING;
	    }
	    return;

	} else if (status == OBSERVING) {
	    observe();
	    return;
	} else if (status == DANCING) {
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

	if (randomBee.status == DANCING) {
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
	    status = FORAGING;
	}
    }

    /**
     * Method called, when a bee fails to select a new source during observation
     * phase and is dispatched to forage from a random source.
     */
    protected void dispatch() {

	source = crawler.randomSource();
	status = FORAGING;
    }

    /**
     * Is invoked when a bee decides to follow a different bee during
     * observation phase, effectively adopting a new source and starting to
     * forage from it. Default behavior is to follow only dancing bees.
     * 
     * @param bee the bee to be followed
     */
    protected void follow(Bee bee) {

	if (bee.status == DANCING) {
	    this.source = bee.source;
	    this.status = FORAGING;
	}
    }

    /**
     * Represents a single random decision of bee whether to dance for the
     * current source, based on current desire value of a bee. Under default
     * behavior, the probability of propagating (dancing for) a source is
     * directly proportional to the current {@link Bee#desire}.
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
     * {@link Bee#desire}</i>.
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
     * implemented in derived class.
     * 
     * @return quality of currently visited source as a value from the interval
     *         of <code><0;1></code>
     */
    protected abstract double evalQuality();

    /**
     * Evaluates the desire of bee to dance / retain the currently visited
     * source. May adjust {@link Bee#quality} by incorporating additional
     * dynamic factors, such as number of bees on a given source at the time of
     * calculation, etc. Current default behavior is not to adjust quality.
     * 
     * @return value from the interval of <code><0;1></code>
     */
    protected double evalDesire(double quality) {

	double desire;

	// TODO: rewrite to implement counters of bees at sources, without iterating over bees

	// desire disabled
	desire = quality;
	// desire = quality - ( crawler.beesAtSource(source) *
	// crawler.DESIRE_REDUCTION );

	if (desire < 0) {
	    desire = 0;
	}

	return desire;
    }

    /**
     * Calculate the initial length of a propagation for a given source.
     * 
     * @return the length of propagation in rounds
     * @see Bee#getTimeToObserve()
     */
    protected int getTimeToDance() {

	int timeToDance = (int) Math.ceil(crawler.MAX_DANCE_TIME * quality);
	return timeToDance;
    }

    /**
     * Calculate the initial length of observation.
     * 
     * @return the length of observation in rounds
     * @see Bee#getTimeToDance()
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