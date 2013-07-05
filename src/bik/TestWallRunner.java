package bik;

import static org.junit.Assert.assertEquals;
import robocode.BattleResults;
import robocode.control.snapshot.IDebugProperty;
import robocode.control.snapshot.IRobotSnapshot;
import robocode.control.testing.RobotTestBed;
import robocode.control.events.BattleCompletedEvent;
import robocode.control.events.RoundStartedEvent;
import robocode.control.events.TurnEndedEvent;

/**
 * Illustrates JUnit testing of Robocode robots.
 * 
 * This test checks to see if the WallRunner behaves as planning.
 * <ol>
 * <li>Hits the wall on start</li>
 * <li>
 * </ol>
 * 
 * @author Bret Ikehara
 * 
 */
public class TestWallRunner extends RobotTestBed {

  int robotPhase = 0;
  boolean robotOnWall = false;
  boolean robotVelocityPositive = false;
  boolean robotVelocityNegative = false;
  boolean robotNewTarget = false;
  boolean enemyOnTheWall = false;
  String closestWall = "none";

  /**
   * Specifies that SittingDuck and WallRunner are to be matched up in this test case.
   * 
   * @return The comma-delimited list of robots in this match.
   */
  @Override
  public String getRobotNames() {
    return "sample.Walls,bik.WallRunner";
  }

  @Override
  public void onRoundStarted(RoundStartedEvent event) {
    IRobotSnapshot robot = event.getStartSnapshot().getRobots()[1];
    closestWall = (robot.getX() > (double) this.width / 2) ? "Right" : "Left";
  }

  /**
   * This test runs for 10 rounds.
   * 
   * @return The number of rounds.
   */
  @Override
  public int getNumRounds() {
    return 10;
  }

  /**
   * The actual test, which asserts that WallRunner has won every round against SittingDuck.
   * 
   * @param event Details about the completed battle.
   */
  @Override
  public void onBattleCompleted(BattleCompletedEvent event) {
    // Return the results in order of getRobotNames.
    BattleResults[] battleResults = event.getIndexedResults();
    // Sanity check that results[1] is WallRunner (not strictly necessary, but illustrative).
    BattleResults wallRunnerResults = battleResults[1];
    String robotName = wallRunnerResults.getTeamLeaderName();
    assertEquals("Check that results[1] is WallRunner", "bik.WallRunner*", robotName);

    boolean winAmount = (wallRunnerResults.getFirsts() >= 0) ? true : false;
    // Check to make sure WallRunner won every round.
    assertEquals("Check WallRunner winner", true, winAmount);

    assertEquals("Moves towards closest wall!", true, this.robotOnWall);
    assertEquals("Moves to phase 1.", 1, this.robotPhase);
    assertEquals("Moves up wall.", true, this.robotVelocityPositive);
    assertEquals("Moves down wall.", true, this.robotVelocityNegative);
    assertEquals("Enemy was on the same wall.", true, this.enemyOnTheWall);
    assertEquals("Find new target after 3 bullet misses in a row.", true, this.robotNewTarget);

  }

  /**
   * Called after each turn. Provided here to show that you could use this method as part of your
   * testing.
   * 
   * @param event The TurnEndedEvent.
   */
  @Override
  public void onTurnEnded(TurnEndedEvent event) {
    IRobotSnapshot robot = event.getTurnSnapshot().getRobots()[1];
    double xPos = robot.getX();

    /*
     * Run through all the debug properties set in the WallRunner.java file.
     */
    for (IDebugProperty debug : robot.getDebugProperties()) {

      if (debug.getKey().compareToIgnoreCase("bulletHit") == 0
          && debug.getValue().compareToIgnoreCase("3") == 0) {
        this.robotNewTarget = true;
      }
      else if (debug.getKey().compareToIgnoreCase("phase") == 0 && this.robotPhase == 0) {
        this.robotPhase = Integer.valueOf(debug.getValue());
      }
      else if (debug.getKey().compareToIgnoreCase("enemyOnTheWall") == 0 && !this.enemyOnTheWall) {
        this.enemyOnTheWall = Boolean.valueOf(debug.getValue());
      }
    }

    /*
     * <ol> <li> Phase 1. Check to see if WallRunner moves to the closest wall. </li> <li> Phase 2.
     * Check to see if WallRunner moves up or down the side of the wall. Note : If a robot is also
     * on the wall, then a check for whether WallRunner moved to the top/bottom of the screen may
     * fail. </il> <li> Phase 2. Check to see if a new target was found because bullets have failed
     * to hit the target three times. </ol>
     */
    if (this.robotPhase == 0) {
      if ((closestWall.compareToIgnoreCase("Right") == 0 && xPos > this.width - 40.0)
          || (closestWall.compareToIgnoreCase("Left") == 0 && xPos < 40.0)) {
        this.robotOnWall = true;
      }
    }
    else if (this.robotPhase == 1) {
      if (!this.robotVelocityNegative && robot.getVelocity() < 0.0) {
        this.robotVelocityNegative = true;
      }
      else if (!this.robotVelocityPositive && robot.getVelocity() > 0.0) {
        this.robotVelocityPositive = true;
      }
    }
  }

  /**
   * Returns a comma or space separated list like: x1,y1,heading1, x2,y2,heading2, which are the
   * coordinates and heading of robot #1 and #2. So "0,0,180, 50,80,270" means that robot #1 has
   * position (0,0) and heading 180, and robot #2 has position (50,80) and heading 270.
   * 
   * Override this method to explicitly specify the initial positions for your test cases.
   * 
   * Defaults to null, which means that the initial positions are determined randomly. Since battles
   * are deterministic by default, the initial positions are randomly chosen but will always be the
   * same each time you run the test case.
   * 
   * @return The list of initial positions.
   */
  @Override
  public String getInitialPositions() {
    return null;
  }

  /**
   * Returns true if the battle should be deterministic and thus robots will always start in the
   * same position each time.
   * 
   * Override to return false to support random initialization.
   * 
   * @return True if the battle will be deterministic.
   */
  @Override
  public boolean isDeterministic() {
    return true;
  }

  /**
   * Specifies how many errors you expect this battle to generate. Defaults to 0. Override this
   * method to change the number of expected errors.
   * 
   * @return The expected number of errors.
   */
  @Override
  protected int getExpectedErrors() {
    return 0;
  }

  /**
   * Invoked before the test battle begins. Default behavior is to do nothing. Override this method
   * in your test case to add behavior before the battle starts.
   */
  @Override
  protected void runSetup() {
    // Default does nothing.
  }

  /**
   * Invoked after the test battle ends. Default behavior is to do nothing. Override this method in
   * your test case to add behavior after the battle ends.
   */
  @Override
  protected void runTeardown() {
    // Default does nothing.
  }

}
