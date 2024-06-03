import java.util.*;
import java.io.*;

public class World2D {
	public static int time;
	public static int population;
	public static int avg_population;
	public static int actions[];
	public static Random rand = new Random(WorldParams.version);
	public static Vector<Genome> v = new Vector<>(); // Storage for all genomes
	public static Cell[][] cWorld;
	public static int grQuant;
	public static Grass[] grassPile;
	public static MapFrame mapFrame = new MapFrame();
	// public static ControlWindow controlWindow = new ControlWindow();
	public static final Object worldLock = new Object();
	public static volatile boolean running = true;
	public static volatile boolean step = false;
	public static int T = WorldParams.age; // Total number of iterations
	public static int maxAgent = 0; // ID of the next agent
	public static int totalAgents = 0; // Number of agents
	public static Genome bur, bur1;
	public static int worldEnergy = 0;

	public World2D() {
		cWorld = new Cell[WorldParams.worldXsize][WorldParams.worldYsize];
		grQuant = (int) (WorldParams.worldXsize * WorldParams.worldYsize * WorldParams.grassIntencity / 100);
		grassPile = new Grass[grQuant];
		actions = new int[Genome.nAct];
		Arrays.fill(actions, 0);
		avg_population = 0;

		initializeWorld();
		initializeGrass();
		initializeAgents();
		setupGUI();
	}

	private void initializeWorld() {
		time = 0;
		population = 0;
		for (int x = 0; x < WorldParams.worldXsize; x++) {
			for (int y = 0; y < WorldParams.worldYsize; y++) {
				cWorld[x][y] = new Cell();
			}
		}
	}

	private void initializeGrass() {
		for (int i = 0; i < grQuant; i++) {
			grassPile[i] = new Grass();
			grassPile[i].x = rand.nextInt(WorldParams.worldXsize);
			grassPile[i].y = rand.nextInt(WorldParams.worldYsize);
			if (!cWorld[grassPile[i].x][grassPile[i].y].hereIsGrass) {
				grassPile[i].age = rand.nextInt(WorldParams.grassCycle);
				cWorld[grassPile[i].x][grassPile[i].y].hereIsGrass = true;
			}
		}
	}

	private void initializeAgents() {
		// Filling the world randomly with initial population of agents.
		// If here is agent in the cell the new random number is chosen.

		String fileName = "weights.txt";
		while (maxAgent < WorldParams.initPopulation) {
			try {
				bur = new Genome(WorldParams.maxEnergy, fileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
			bur.dir = rand.nextInt(3);
			bur.id = maxAgent;
			bur.x = rand.nextInt(WorldParams.worldXsize);
			bur.y = rand.nextInt(WorldParams.worldYsize);
			bur.stackID = maxAgent;
			bur.dividePrevious = 0;
			bur.divideLast = 0;
			bur.age = 0;
			bur.generation = 0;
			cWorld[bur.x][bur.y].agents.addElement(bur);
			v.addElement(bur);
			maxAgent++;
			System.out.println("Agent #" + maxAgent + " generated");
		}
		totalAgents = maxAgent; // Initially total number of agents is equal to the id of next agent
	}

	private void setupGUI() {
		// Creating Map
		mapFrame.setVisible(true);
		// controlWindow.setVisible(true);
	}

	public void evolve(FileWriter f, BufferedReader r) throws IOException {
		int action; // Action performing
		LogSave simLog = new LogSave();
		FileWriter saveGenome; // Pointer to log file with genomes
		Agent neuralNet = new Agent();
		FieldOfVision curFieldOfVision = new FieldOfVision();
		World2D.population = totalAgents;

		// evolution starts <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		for (int t = 0; t < T; t++) {
			synchronized (World2D.worldLock) {
				while (!World2D.running && !World2D.step) {
					try {
						World2D.worldLock.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
				if (World2D.step) {
					World2D.step = false;
				}

				time = t;
				int ttl = totalAgents;

				// !!!!!!!!!!!!!!!!!!!!!!!!!!
				//if (t % 500000 == 0)
				// WorldParams.grassCycle -= 500;
				// !!!!!!!!!!!!!!!!!!!!!!!!

				if (t % 5000 == 0)
					System.out.println(t + " # " + totalAgents + " E: " + WorldParams.grassCycle);

				// permutations of order of execution of agents
				int[] agentExct = new int[totalAgents];
				agentExct = mixPopulation(agentExct);

				for (int zzz = 0; zzz < ttl; zzz++) { // For every agent do...
					int z;
					z = agentExct[zzz];
					bur = (Genome) v.elementAt(z); // bur - current agent

					// filling Field of agent's vision
					curFieldOfVision.fillingFieldOfVision(cWorld, bur.x, bur.y, bur.dir);

					cWorld[bur.x][bur.y].agents.remove(bur); // removing agent from cell
					int cellAgntSize = cWorld[bur.x][bur.y].agents.size();

					// searching neighbour
					bur.cellNeighbourKinship = 0; // nonkinship - distance in marker space
					bur.cellNeighbourID = -1;
					bur.cellNeighbourEnergy = 0;
					bur.neighbourID = -1;
					if (cellAgntSize > 0)
						searchNeighbour(bur, cellAgntSize);

					// getting action to be perfofmed
					// bur.act = neuralNet.DoAction(curFieldOfVision, bur, WorldParams.maxEnergy);
					bur = neuralNet.DoActionDetails(curFieldOfVision, bur, WorldParams.maxEnergy);
					action = bur.act;
					actions[action]++; // accumulata statictics of acitions
					bur.deltaE = bur.energy;

					switch (action) {
						case 0: // resting
							bur = actRest(bur);
							break;
						case 1: // eating
							bur = actEat(bur);
							break;
						case 2: // moving
							bur = actMove(bur);
							break;
						case 3: // turning left
							bur = actTurnLeft(bur);
							break;
						case 4: // turning right
							bur = actTurnRight(bur);
							break;
						case 5: // division
							bur = actDivide(bur);
							break;
						case 6: // fighting
							bur = actFight(bur, cellAgntSize);
							break;
						case 7: // sharing
							bur = actShare(bur, cellAgntSize);
							break;
						default:
							// Handle any actions not covered by the cases
							break;
					}

					// calculating energy loss
					bur.deltaE = bur.energy - bur.deltaE;
					bur.age++;

					// Storing agent in the same position in the vector after performing the action
					cWorld[bur.x][bur.y].agents.add(bur);
					v.setElementAt(bur, z);

				} // all agents made their actions

				// If energy less than zero agent dies
				cleanDeadAgents();

				// setting agents stackID's
				setStackIDs();

				population = totalAgents;
				avg_population += population;

				// Grass refreshing every grassCycle
				updateGrass();

				// Saving number of agents performing certain actions in file agents.txt
				if (t % WorldParams.period == 0) {
					simLog.saveLog(v, (int) (avg_population / WorldParams.period), t);
					Arrays.fill(actions, 0);
					avg_population = 0;
				}

				// Saving average genome
				if (t % WorldParams.saveAverGenPeriod == 0) {
					simLog.saveAverGen();
				}

				// Saving genomes in log file each savePeriod
				if (t % WorldParams.savePeriod == 0) {
					saveGenome = new FileWriter("log" + t + ".txt");
					for (int z = 0; z < totalAgents; z++) {
						bur = (Genome) v.elementAt(z);
						bur.saveGenome(saveGenome);
					}
				}
			}

			// Refreshing MAP
			if (MapFrame.isPleaseDraw()) {
				mapFrame.triggerRepaint();
			}

		} // <<<<<<< END of ITERATION >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

		simLog.closeLog();
		saveFinalStats(f);
		mapFrame.removeAll();
		mapFrame.dispose();
	}

	private int[] mixPopulation(int[] agentExct) {
		// permutations of order of execution of agents
		int p1, p2, valP;
		for (int z = 0; z < totalAgents; z++) {
			agentExct[z] = z;
		}
		for (int z = 0; z < totalAgents; z++) {
			p1 = rand.nextInt(totalAgents);
			p2 = rand.nextInt(totalAgents);
			if (p1 != p2) { // swaping
				valP = agentExct[p1];
				agentExct[p1] = agentExct[p2];
				agentExct[p2] = valP;
			}
		}
		return agentExct;
	}

	private void searchNeighbour(Genome bur, int cellAgntSize) {
		bur.cellNeighbourID = rand.nextInt(cellAgntSize);
		bur1 = (Genome) cWorld[bur.x][bur.y].agents.elementAt(bur.cellNeighbourID);
		bur.cellNeighbourEnergy = bur1.energy;
		bur.neighbourID = bur1.id;

		// calculating marker distance between agents
		for (int mark = 0; mark < WorldParams.markerLength; mark++) {
			bur.cellNeighbourKinship = bur.cellNeighbourKinship +
					(int) Math.pow((bur.marker[mark] - bur1.marker[mark]), 2);
		}
		bur.cellNeighbourKinship = (int) (Math.sqrt(bur.cellNeighbourKinship));
	}

	private Genome actRest(Genome bur) {
		bur.energy -= WorldParams.eRest;
		return bur;
	}

	private Genome actEat(Genome bur) {
		bur.energy -= WorldParams.eEat;
		if (cWorld[bur.x][bur.y].hereIsGrass) {
			bur.energy += WorldParams.eGrass;
			// Energy cannot be more than maximum
			if (bur.energy > WorldParams.maxEnergy)
				bur.energy = WorldParams.maxEnergy;
			cWorld[bur.x][bur.y].hereIsGrass = false;
		}
		return bur;
	}

	private Genome actMove(Genome bur) {
		bur.energy -= WorldParams.eMove;
		int xNew, yNew;
		switch (bur.dir) {
			case 0: // moving up
				if (bur.x == 0)
					xNew = WorldParams.worldXsize - 1;
				else
					xNew = bur.x - 1;
				bur.x = xNew;
				break;
			case 1:// moving right
				yNew = (bur.y + 1) % WorldParams.worldYsize;
				bur.y = yNew;
				break;
			case 2:// moving down
				xNew = (bur.x + 1) % WorldParams.worldXsize;
				bur.x = xNew;
				break;
			case 3:// moving left
				if (bur.y == 0)
					yNew = WorldParams.worldYsize - 1;
				else
					yNew = bur.y - 1;
				bur.y = yNew;
				break;
		}
		return bur;
	}

	private Genome actTurnLeft(Genome bur) {
		bur.energy -= WorldParams.eTurn;
		bur.dir = (bur.dir + 3) % 4;
		return bur;
	}

	private Genome actTurnRight(Genome bur) {
		bur.energy -= WorldParams.eTurn;
		bur.dir = (bur.dir + 1) % 4;
		return bur;
	}

	private Genome actDivide(Genome bur) {
		// Division takes the half of parent's energy to the offspring
		bur.energy -= WorldParams.eDivide;
		bur.dividePrevious = bur.divideLast;
		bur.divideLast = time;
		if (v.size() < (WorldParams.worldXsize * WorldParams.worldYsize * 100)) {
			// bur.energy = bur.energy / 2;
			bur1 = new Genome(bur, WorldParams.mutation, WorldParams.mutModul, WorldParams.maxWeight,
					rand);
			bur1.id = maxAgent;
			// bur1.energy = bur.energy;
			bur1.x = bur.x;
			bur1.y = bur.y;
			bur1.divideLast = time;
			bur1.dividePrevious = time;
			bur1.generation = bur.generation + 1;
			totalAgents += 1;
			maxAgent += 1;
			bur1.stackID = totalAgents - 1;

			if (bur.energy < WorldParams.eShare) { // an agent can't share more resources than he has
				bur1.energy = bur.energy;
				bur.energy -= WorldParams.eShare;
			} else {
				bur1.energy = WorldParams.eShare;
				bur.energy -= WorldParams.eShare;
			}

			cWorld[bur1.x][bur1.y].agents.add(bur1);
			v.addElement(bur1); // Add new agent to the end of the vector
		}
		return bur;
	}

	private Genome actFight(Genome bur, int cellAgntSize) {
		bur.energy -= (int) WorldParams.eFight/2;
		if (cellAgntSize > 0) {
			bur1 = (Genome) cWorld[bur.x][bur.y].agents.elementAt(bur.cellNeighbourID);
			if (bur1.energy < (WorldParams.eFight)) {
				bur.energy += bur1.energy;
				bur1.energy -= WorldParams.eFight;
			} else {
				bur.energy += WorldParams.eFight;// 1.5*WorldParams.eFight;
				bur1.energy -= WorldParams.eFight;
			}
			if (bur.energy > WorldParams.maxEnergy) {
				bur.energy = WorldParams.maxEnergy;
			}
			cWorld[bur1.x][bur1.y].agents.setElementAt(bur1, bur.cellNeighbourID);
			v.setElementAt(bur1, bur1.stackID);
		}
		return bur;
	}

	private Genome actShare(Genome bur, int cellAgntSize) {
		bur.energy -= WorldParams.eTurn;
		if (cellAgntSize > 0) {
			bur1 = (Genome) cWorld[bur.x][bur.y].agents.elementAt(bur.cellNeighbourID);
			if (bur.energy < WorldParams.eShare) { // an agent can't share more resources than he has
				bur1.energy += bur.energy;
				bur.energy -= WorldParams.eShare;
			} else {
				bur1.energy += WorldParams.eShare;
				bur.energy -= WorldParams.eShare;
			}
			if (bur1.energy > WorldParams.maxEnergy) {
				bur1.energy = WorldParams.maxEnergy;
			}
			cWorld[bur1.x][bur1.y].agents.setElementAt(bur1, bur.cellNeighbourID);
			v.setElementAt(bur1, bur1.stackID);
		}
		return bur;
	}

	private void cleanDeadAgents() {
		// If energy less than zero agent dies
		for (int z = totalAgents; z > 0; z--) {
			bur = (Genome) v.elementAt(z - 1);
			if (bur.energy <= 0) {
				cWorld[bur.x][bur.y].agents.remove(bur);
				v.removeElementAt(z - 1);
				totalAgents--;
			}
		}
	}

	private void setStackIDs() {
		// setting agents stackID's
		for (int z = totalAgents; z > 0; z--) {
			bur = (Genome) v.elementAt(z - 1);
			cWorld[bur.x][bur.y].agents.remove(bur);
			bur.stackID = z - 1;
			v.setElementAt(bur, bur.stackID);
			cWorld[bur.x][bur.y].agents.add(bur);
		}
	}

	private void updateGrass() {
		// Grass refreshing every grassCycle
		// grass in array
		int i, j;
		worldEnergy = 0;
		for (int g = 0; g < grQuant; g++) {
			grassPile[g].age++;
			if (grassPile[g].age == WorldParams.grassCycle) {
				cWorld[grassPile[g].x][grassPile[g].y].hereIsGrass = false;
				do {
					i = rand.nextInt(WorldParams.worldXsize);
					j = rand.nextInt(WorldParams.worldYsize);
					grassPile[g].x = i;
					grassPile[g].y = j;
				} while (cWorld[grassPile[g].x][grassPile[g].y].hereIsGrass);
				cWorld[grassPile[g].x][grassPile[g].y].hereIsGrass = true;
				grassPile[g].age = rand.nextInt(WorldParams.grassCycle);
			}
			if (cWorld[grassPile[g].x][grassPile[g].y].hereIsGrass)
				worldEnergy++;
		}
	}

	public void saveFinalStats(FileWriter f) throws IOException {
		int nIn = Genome.nIn;
		int nAct = Genome.nAct;

		double[][] weight = new double[nIn][nAct]; // Average weight matrix of final population
		int[] hasinput = new int[nIn];
		int[] hasaction = new int[nAct];
		for (int z = 0; z < totalAgents; z++) {
			bur = (Genome) v.elementAt(z);
			f.write("#: " + z + "\tID: " + bur.id + "\tE: " + bur.energy + "\tA: " + bur.act + "\tdeltaE " + bur.deltaE
					+ "\r\n");
			for (int xx = 0; xx < nIn; xx++) {
				for (int yy = 0; yy < nAct; yy++) {
					weight[xx][yy] += bur.weight[xx][yy];
					if (bur.action[yy])
						hasaction[yy]++;
				}
				if (bur.input[xx])
					hasinput[xx]++;
			}
		}
		f.write("Actions\r\n");
		for (int xx = 0; xx < nIn; xx++) {
			for (int yy = 0; yy < nAct; yy++) {
				if (totalAgents != 0)
					f.write((int) weight[xx][yy] / totalAgents + "\t");
				else
					f.write("#\t");
			}
			f.write(hasinput[xx] + "\r\n");
		}
		for (int yy = 0; yy < nAct; yy++)
			f.write(hasaction[yy] / nIn + "\t");
	}

} // end World2D