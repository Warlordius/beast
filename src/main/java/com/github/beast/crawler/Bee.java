package beast.crawler;

import java.util.*;

import beast.page.Page;

public abstract class Bee extends Thread {

	public static final boolean ANNOUNCE = false;
	public static final int FORAGING = 0;
	public static final int OBSERVING = 1;
	public static final int DANCING = 2;
	public static final double ADD_THRESHOLD = 0.3;

	public Page source;
	public Page newSource;
	public String keyword;
	public Crawler crawler;
	public int status;
	public double quality;
	public double newQuality;
	public double desire;
	public int timeToDance = 0;
	public int timeToObserve = 0;
	public boolean newDance = false;

	public Bee(Crawler crawler) {

		this.crawler = crawler;
	}

	/**
	 * Initializes a bee. Default initialization consists of selecting a random
	 * source from currently available sources and subsequently forage for it.
	 */
	public void initBee() {

		this.source = crawler.randomSource();
		this.status = FORAGING;

	}

	/**
	 * Performs a single iteration of bee. Method encapsulates the decision tree
	 * of a bee, deciding on whether to dance, forage, or observe in the next
	 * iteration.
	 */
	final public void doIteration() {

		if (status == FORAGING) {

			forage();

			if (decideToLeave()) {
				timeToObserve = getTimeToObserve();
				status = OBSERVING;

			} else if (decideToDance()) {
				timeToDance = getTimeToDance();
				newDance = true;
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

	protected void forage() {

		doWhileForaging();
	}

	/**
	 * Method encapsulating foraging behavior of a bee. The method is invoked,
	 * if a bee decides to forage for its source.
	 */
	protected abstract void doWhileForaging();

	/**
	 * Method encapsulating observing behavior of a bee. First a bee to be
	 * followed is selected. If this bee is dancing, it is followed. If the bee
	 * is not dancing, nothing happens in this round, except decreasing the time
	 * to observe.
	 * 
	 * If the time to observe runs out, the bee is dispatched, thus ending the
	 * observation phase.
	 */
	public void observe() {

		timeToObserve--;

		doWhileObserving();

		Random generator = new Random();
		int randBeeNum = generator.nextInt(crawler.bees.size());
		Bee randBee = crawler.bees.get(randBeeNum);

		if (randBee.status == DANCING) {
			follow(randBee);
		}

		if (timeToObserve <= 0) {
			dispatch();
		}
	}

	protected void doWhileObserving() {

	}

	protected void dance() {

		timeToDance--;

		doWhileDancing();

		if (timeToDance <= 0) {
			status = FORAGING;
		}
	}

	protected void doWhileDancing() {

	}

	public void dispatch() {

		source = crawler.randomSource();
		keyword = crawler.index.getRandKeyword(source);
		status = FORAGING;
	}

	protected void follow(Bee bee) {

		if (bee.status == DANCING) {
			this.status = FORAGING;
			this.source = bee.source;
		}
	}

	/*
	 * public double evalQuality() { if (!source.processed) { source.process();
	 * } if (!source.indexed) { crawler.index.indexPage(source); }
	 * 
	 * Date now = new Date(); long delay = now.getTime() -
	 * source.timestamp.getTime(); double base = 3.2; double factor = 3600000;
	 * double quality = 1 / ( ( ( Math.pow(2.71828, ( Math.log(delay) /
	 * Math.log(base) ) ) ) / factor ) + 1 ); double enhance = 0;
	 * 
	 * source.quality = quality + ((1-quality)*enhance); return source.quality;
	 * }
	 */

	public double evalDesire(double quality) {

		double desire;

		// desire disabled
		desire = quality;
		// desire = quality - ( crawler.beesAtSource(source) *
		// crawler.DESIRE_REDUCTION );

		if (desire < 0) {
			desire = 0;
		}

		return desire;
	}

	protected abstract double evalQuality();

	public boolean decideToLeave() {

		Random generator = new Random();
		if (generator.nextDouble() > desire) {
			return true;
		}
		return false;
	}

	public boolean decideToDance() {

		Random generator = new Random();
		if (generator.nextDouble() > desire) {
			return false;
		}
		return true;
	}

	public int getTimeToDance() {

		int ttd = (int) Math.ceil(crawler.MAX_DANCE_TIME * quality);
		return ttd;
	}

	public int getTimeToObserve() {

		return crawler.MAX_OBSERVE_TIME;
	}
}