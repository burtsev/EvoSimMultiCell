// Object Agent generates action at conditions of enviroment and genome

public class Agent {
  public static int[] inputValue;
  public static int[] outputValue;
  private static int kC = (int) Math.sqrt(WorldParams.markerLength);

  Agent() {
  };

  public int DoAction(FieldOfVision vision, Genome gnm, int maxEnergy) {
    int maxSum, maxN;
    int nIn = Genome.nIn;
    int nAct = Genome.nAct;

    // filling vector of input values
    inputValue = new int[nIn];
    outputValue = new int[nAct];

    int k = maxEnergy;
    for (int i = 0; i < nIn; i++) {
      inputValue[i] = 0;
    }
    inputValue[0] = k;
    if (vision.near.hereIsGrass)
      inputValue[1] = k;
    if (vision.left.hereIsGrass)
      inputValue[2] = k;
    if (vision.forward.hereIsGrass)
      inputValue[3] = k;
    if (vision.right.hereIsGrass)
      inputValue[4] = k;
    inputValue[5] = vision.near.agents.size() * 1000;
    inputValue[6] = vision.left.agents.size() * 1000;
    inputValue[7] = vision.forward.agents.size() * 1000;
    inputValue[8] = vision.right.agents.size() * 1000;
    vision.near.calcKin(gnm);
    inputValue[9] = vision.near.kinship;
    inputValue[10] = gnm.energy;
    inputValue[11] = maxEnergy - gnm.energy;
    inputValue[12] = (int) gnm.cellNeighbourKinship/kC;
    inputValue[13] = gnm.cellNeighbourEnergy;
    inputValue[14] = maxEnergy - gnm.cellNeighbourEnergy;
    // inputValue[18] = gnm.deltaE * (int)(k/WorldParams.eGrass);
    // end filling

    maxSum = Integer.MIN_VALUE;
    maxN = 0;
    // sum = new int[nAct];
    int[][] weight = gnm.weight;
    for (int j = 0; j < nAct; j++) {
      if (gnm.action[j]) {
        outputValue[j] = 0;
        for (int i = 0; i < nIn; i++) {
          if (gnm.input[i])
            outputValue[j] = outputValue[j] + weight[i][j] * inputValue[i];
        }
        if (outputValue[j] > maxSum) {
          maxN = j;
          maxSum = outputValue[j];
        }
      }
    }
    return (maxN);
  } // end of DoAction

  public Genome DoActionDetails(FieldOfVision vision, Genome gnm, int maxEnergy) {
    int maxSum;

    // filling vector of input values
    int k = maxEnergy;
    int many = 5;
    int k2 = (int) k / many;
    for (int i = 0; i < gnm.nIn; i++) {
      gnm.inputValue[i] = 0;
    }
    gnm.inputValue[0] = k;
    if (vision.near.hereIsGrass)
      gnm.inputValue[1] = k;
    if (vision.left.hereIsGrass)
      gnm.inputValue[2] = k;
    if (vision.forward.hereIsGrass)
      gnm.inputValue[3] = k;
    if (vision.right.hereIsGrass)
      gnm.inputValue[4] = k;
    gnm.inputValue[5] = Math.min(vision.near.agents.size(), many) * k2;
    gnm.inputValue[6] = Math.min(vision.left.agents.size(), many) * k2;
    gnm.inputValue[7] = Math.min(vision.forward.agents.size(), many) * k2;
    gnm.inputValue[8] = Math.min(vision.right.agents.size(), many) * k2;
    vision.near.calcKin(gnm);
    gnm.inputValue[9] = vision.near.kinship;
    gnm.inputValue[10] = gnm.energy;
    gnm.inputValue[11] = maxEnergy - gnm.energy;
    //gnm.inputValue[12] = (int) (gnm.cellNeighbourKinship * k / (2 * WorldParams.maxMarkerValue));
    gnm.inputValue[12] = (int) gnm.cellNeighbourKinship/kC;
    gnm.inputValue[13] = gnm.cellNeighbourEnergy;
    gnm.inputValue[14] = maxEnergy - gnm.cellNeighbourEnergy;
    // gnm.inputValue[18] = gnm.deltaE * (int)(k/WorldParams.eGrass);
    // end filling

    maxSum = Integer.MIN_VALUE;
    for (int j = 0; j < gnm.nAct; j++) {
      if (gnm.action[j]) {
        gnm.outputValue[j] = 0;
        for (int i = 0; i < gnm.nIn; i++) {
          if (gnm.input[i])
            gnm.outputValue[j] = Math.max(0, gnm.outputValue[j] + gnm.weight[i][j] * gnm.inputValue[i]);
        }
        if (gnm.outputValue[j] > maxSum) {
          gnm.act = j;
          maxSum = gnm.outputValue[j];
        }
      }
    }
    return (gnm);
  } // end of DoAction
} // end of Agent class