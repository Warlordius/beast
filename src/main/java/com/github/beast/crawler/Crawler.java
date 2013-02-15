package beast.crawler;

import java.util.*;

import beast.Model;
import beast.indexNew.BeastIndex;
import beast.page.Page;

public class Crawler {

	public final int BEE_NUMBER = 50;
	public final int ITERATIONS = 10000;
	public final int MAX_DANCE_TIME = 3;
	public final int MAX_OBSERVE_TIME = 3;
	public final boolean ANNOUNCE = true;
	public final double DECAY = 0.8;
	public final double DESIRE_REDUCTION = 0.000;
	public final long DELAY = 5000;

	protected ArrayList<Bee> bees;
	protected BeastIndex index;

	public Crawler(BeastIndex index) {

		this.index = index;
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
			newBee.initBee();
			bees.add(newBee);
		}
	}

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

			for (int j = 0; j < bees.size(); j++)
				bees.get(j).doIteration();

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				System.out.println("Unexpected interruption of crawler system");
			}
		}
	}

	Bee getBee(int num) {

		return bees.get(num);
	}

	public Page randomSource() {

		return index.getRandPage();
	}

	public int beesAtSource(Page page) {

		Iterator<Bee> itr = bees.iterator();
		int count = 0;

		while (itr.hasNext()) {
			if (itr.next().source == page) {
				count++;
			}
		}

		return count;
	}

	public double getAvgQuality() {

		int count = 0;
		double sum = 0;

		for (int i = 0; i < index.pages.size(); i++) {
			if (index.pages.get(i).quality > 0) {
				count++;
				sum = sum + index.pages.get(i).quality;
			}
		}

		return (sum / count);
	}

	public String iterationStats(int i) {

		int foragingBees = 0;
		int dancingBees = 0;
		int observingBees = 0;

		for (int j = 0; j < bees.size(); j++) {
			if (bees.get(j).status == Bee.FORAGING)
				foragingBees++;
			else if (bees.get(j).status == Bee.DANCING)
				dancingBees++;
			else if (bees.get(j).status == Bee.OBSERVING) observingBees++;
		}
		String stats = String.format("iteration %04d, foraging bees %03d, dancing bees %03d, observing bees %03d", i, foragingBees, dancingBees,
				observingBees);
		Model.log(stats);

		return stats;
	}
}