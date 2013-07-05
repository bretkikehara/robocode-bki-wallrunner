package bik;

import java.awt.Color;
import robocode.BulletHitEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;

/**
 * Wall Runner Robot.
 * 
 * @author breti
 * @version 1.0
 */
public class WallRunner extends Robot {

  String enemyName = null;
  double enemyDistance;
  boolean enemyOnTheWall;
  boolean hitWall = false;
  double centerX;
  double centerY;
  int phase = 0;
  int bulletHit = 0;
  double distance = 0.0;

  /**
   * Overrided run method.
   */
  public void run() {
    double turnAngle;

    this.setColors(Color.red, Color.blue, Color.yellow);
    this.enemyOnTheWall = false;

    this.distance = 100.0;
    this.centerY = this.getBattleFieldHeight() / 2;
    this.centerX = this.getBattleFieldWidth() / 2;

    this.setDebugProperty("phase", String.valueOf(this.phase));

    // Send robot to the left/right side of screen, whichever is closer
    turnAngle = (this.getX() > this.centerX) ? 90.0 : 270.0;
    turnAngle = this.turnAngle(turnAngle - this.getHeading());
    this.turnRight(turnAngle);

    while (this.phase == 0) {
      this.ahead(1000.0);
      if (this.hitWall && (this.getX() < 40.0 || this.getX() > this.getBattleFieldWidth() - 40.0)) {
        this.phase = 1;
      }
    }

    this.setDebugProperty("phase", String.valueOf(this.phase));
    this.setDebugProperty("bulletHit", String.valueOf(this.bulletHit));

    // Turn radar south when false, north when true.
    this.turnRadarLeft(this.getRadarHeading());
    while (true) {

      this.turnRadarRight(360.0);
      this.ahead(this.distance);
    }
  }

  /**
   * Handle this robot hitting the wall.<br/>
   * <b>Phase 0:</b> Turn this robot North.<br/>
   * <b>Phase 1:</b> Reverse the robot's direction.<br/>
   * 
   * @param event HitWallEvent
   */
  public void onHitWall(HitWallEvent event) {
    double turnAngle;
    if (this.phase == 0) {
      // Face robot North.
      turnAngle = this.turnAngle(this.getHeading());
      this.turnLeft(turnAngle);
      this.hitWall = true;
    }
    else if (this.phase == 1) {
      // Reverse this robot's direction.
      this.distance *= -1.0;
    }
  }

  /**
   * Handle this robot's collision with enemy robot.<br/>
   * <b>Phase 0:</b> Fire at enemy, then move back turn towards the center of the screen and go
   * ahead 100 pixels.<br/>
   * 
   * @param event HitRobotEvent
   */
  public void onHitRobot(HitRobotEvent event) {
    double turnAngle, heading;
    if (this.phase == 0) {
      this.hitWall = false;
      // Continue in the direction
      heading = this.getHeading();

      // Turn robot towards the center.
      turnAngle = (this.getY() > this.centerY) ? 90.0 : -90.0;
      turnAngle = (this.getX() > this.centerX) ? turnAngle * -1.0 : turnAngle;
      this.turnLeft(turnAngle);
      this.ahead(100.0);

      // Return this robot to original heading.
      turnAngle = (this.getY() > centerY) ? 180.0 - heading : heading;
      turnAngle = this.turnAngle(turnAngle - this.getHeading());
      this.turnRight(turnAngle);
    }
    else if (this.phase == 1) {
      // Rams into the enemy
      this.back(this.distance);
      this.ahead(this.distance);
    }

  }

  /**
   * Handle when enemy robot is found on radar.
   * 
   * @param event ScannedRobotEvent
   */
  public void onScannedRobot(ScannedRobotEvent event) {

    double gunAngle;
    if( this.phase == 0) {
      this.resume();
    }
    else if( this.phase == 1) {    
      // Lock on to the closest enemy.
      if ( this.enemyName == null || (!this.enemyName.equals(event.getName()) && event.getDistance() < this.enemyDistance) ) {
        this.setEnemyLockedOn(event);
      }
  
      if (Math.floor(event.getBearing() % 180.0) == 0) {
        gunAngle = this.turnAngle(event.getBearing() + (this.getHeading() - this.getGunHeading()));
        this.turnGunRight(gunAngle);
        this.enemyOnTheWall = true;
      }
      else {
        this.enemyOnTheWall = false;
      }
  
      this.setDebugProperty("enemyOnTheWall", String.valueOf(this.enemyOnTheWall));
  
      // Move towards enemy if enemy is on the wall.
      if (this.enemyOnTheWall) {
        this.distance = ((event.getBearing() > 90.0) ? -1.0 : 1.0) * Math.abs(this.distance);
        this.distance = ((this.getX() < this.centerX) ? -1.0 : 1.0) * this.distance;
      }
  
      if (this.enemyName.equals(event.getName())) {
        // Turn gun towards enemy while handling change.
        gunAngle = this.turnAngle(event.getBearing() + (this.getHeading() - this.getGunHeading()));
        this.turnGunRight(gunAngle % 360.0);
        this.fire(this.getBulletPower(event));
        this.ahead(this.distance);
      }
    }
  }

  @Override
  public void fire(double power) {
    super.fire(power);

    this.bulletHit++;
    this.setDebugProperty("bulletHit", String.valueOf(this.bulletHit));

    // Three misses, then find new robot
    if (this.bulletHit > 2) {
      this.enemyName = null;
    }
  }

  /**
   * Check if the enemy is dead.
   * 
   * @param event BulletHitEvent
   */
  public void onBulletHit(BulletHitEvent event) {
    this.bulletHit = 0;

    // Remove enemy from lock when enemy dies.
    if (event.getEnergy() <= 0.0) {
      this.enemyName = null;
    }
  }

  /**
   * Sets the details for the enemy robot that is locked on.
   * 
   * @param event ScannedRobotEvent
   */
  public void setEnemyLockedOn(ScannedRobotEvent event) {
    this.enemyName = event.getName();
    this.enemyDistance = event.getDistance();
    this.bulletHit = 0;
  }

  /**
   * Calculates the bullet power based on distance to robot.
   * 
   * @param event ScannedRobotEvent
   * @return double
   */
  public double getBulletPower(ScannedRobotEvent event) {

    double bulletPower = 1.0;

    if (this.enemyOnTheWall) {
      bulletPower = 100.0;
    }
    else {
      bulletPower = this.getBattleFieldWidth() - event.getDistance();
      bulletPower = 4.0 * (bulletPower / this.getBattleFieldWidth());
    }

    this.setDebugProperty("bulletPower", String.valueOf(bulletPower));

    return bulletPower;
  }

  /**
   * Calculates which was to turn this robot. Chooses whichever angle is less.
   * 
   * @param angle double
   * @return double
   */
  public double turnAngle(double angle) {
    double turnAngle = 0.0;
    /*
     * Calculates a negative angle so that turning left will turn right.
     */
    if (angle > 180.0) {
      turnAngle = angle - 360.0;
    }
    else if (angle < -180.0) {
      turnAngle = 360.0 + angle;
    }
    return turnAngle;
  }
}
