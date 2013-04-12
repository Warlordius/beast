package com.github.beast.crawler;

import java.util.*;

import com.github.beast.Beast;
import com.github.beast.database.BeastIndex;
import com.github.beast.page.Page;

public class Crawler {

    public final double DECAY = 0.8;
    public final int MAX_DANCE_TIME = 3;
    public final int MAX_OBSERVE_TIME = 3;
    
    protected ArrayList<Bee> bees;
    protected BeastIndex index;
    
    private final boolean ANNOUNCE = true;
    private final int BEE_NUMBER = 50;
    // private final double DESIRE_REDUCTION = 0.000;
    private final long DELAY = 5000;
    private final int ITERATIONS = 10000;

    public Crawler(BeastIndex index) {

	this.index = index;
    }

//    public int beesAtSource(Page page) {
//
//	Iterator<Bee> itr = bees.iterator();
//	int count = 0;
//
//	while (itr.hasNext()) {
//	    if (itr.next().getSource() == page) {
//		count++;
//	    }
//	}
//
//	return count;
//    }

    public void doCrawl() {

	doCrawl(BEE_NUMBER, ITERATIONS);
    }

    public void doCrawl(int iterations) {

	doCrawl(BEE_NUMBER, iterations);
    }

    public void doCrawl(int beeNum, int iterations) {

		init(beeNum);

		if (ANNOUNCE) System.out.println("Crawling...");

	// crawl body
	for (int i = 0; i < iterations; i++) {

	    if (ANNOUNCE) System.out.println(iterationStats(i));

	    for (int j = 0; j < bees.size(); j++) {
		bees.get(j).doIteration();
	    }
	    
	    try {
		Thread.sleep(DELAY);
	    } catch (InterruptedException e) {
		System.out.println("Unexpected interruption of crawler system");
	    }
	}
    }

    public Bee getBee(int num) {

	return bees.get(num);
    }

    public void init() {

	init(BEE_NUMBER);
    }

    public void init(int beeNum) {

	if (ANNOUNCE) {
	    System.out.println("Initializing crawler...");
	}

	bees = new ArrayList<Bee>();

	for (int i = 0; i < beeNum; i++) {
	    Bee newBee = new NewsBee(this);
	    bees.add(newBee);
	}
    }

    public String iterationStats(int i) {

	int foragingBees = 0;
	int dancingBees = 0;
	int observingBees = 0;

	for (int j = 0; j < bees.size(); j++) {
    	    if (bees.get(j).status == Bee.Status.FORAGING) {
    		foragingBees++;
    	    } else if (bees.get(j).status == Bee.Status.DANCING) {
    		dancingBees++;
    	    } else if (bees.get(j).status == Bee.Status.OBSERVING) {
		observingBees++;
	    }
	}
	String stats = String.format("iteration %04d, foraging bees %03d, dancing bees %03d, observing bees %03d", i, foragingBees, dancingBees,
		observingBees);
	Beast.log(stats);

	return stats;
    }

    public Page randomSource() {

	return index.getRandPage();
    }
}