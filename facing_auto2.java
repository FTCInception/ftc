package org.firstinspires.ftc.teamcode;
/**
 * Created by nplaxton on 10/26/18
 * TODO
 * find pid values
 * Find teammarker servo values
 * wheel ratio value?
 * make stuff less based on guesses of what hardware will be (ie dump)
 * probably some stuff Im forgetting I wanted to fix
 * PRACTICE PRACTICE PRACTICE
 */

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.hardware.bosch.BNO055IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import com.disnodeteam.dogecv.CameraViewDisplay;
import com.disnodeteam.dogecv.DogeCV;
import com.disnodeteam.dogecv.detectors.roverrukus.GoldAlignDetector;
import com.disnodeteam.dogecv.detectors.roverrukus.SamplingOrderDetector;
import com.vuforia.CameraDevice;

@Autonomous(name="facing_auto2", group="facing_auto2")

public class facing_auto2 extends LinearOpMode {


    private static DcMotor l_f_motor, l_b_motor, r_f_motor, r_b_motor, hang_motor;
    private static Servo teamMarker;
    private final int TICS_PER_REV = 1120;
    Orientation lastAngles = new Orientation();
    DigitalChannel          touch;
    BNO055IMU               imu;
    double                  globalAngle, power = .30, correction;
    private final double INCHES_PER_TIC = 0.011214285;
    private final double wheelRatio = (18.18 / 1); //Not actually what we have pretty sure, needs to be changed. Yeah this is completel y wrong - Peter
    PIDController           pidRotate, pidDrive;
    private GoldAlignDetector detector;


    private final short ROT_LEFT = -1;
    private final short ROT_RIGHT = 1;

    public void initialize() {
        l_f_motor = hardwareMap.dcMotor.get("left_front");
        l_f_motor.setDirection(DcMotorSimple.Direction.FORWARD);
        l_b_motor = hardwareMap.dcMotor.get("left_back");
        l_b_motor.setDirection(DcMotorSimple.Direction.FORWARD);
        r_f_motor = hardwareMap.dcMotor.get("right_front");
        r_f_motor.setDirection(DcMotorSimple.Direction.REVERSE);
        r_b_motor = hardwareMap.dcMotor.get("right_back");
        r_b_motor.setDirection(DcMotorSimple.Direction.REVERSE);
        //NOTE In the example the DcMotorController.RunMode was used, but this wasn't working so I changed it to DcMotor.RunMode
        hang_motor = hardwareMap.dcMotor.get("hang_motor");
        hang_motor.setDirection(DcMotorSimple.Direction.REVERSE);
        /*teamMarker servo*/
        //teamMarker = hardwareMap.servo.get("teamMarker");
        //teamMarker.setPosition(.20);//THIS VALUE NEEDS FINDING
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();

        parameters.mode                = BNO055IMU.SensorMode.IMU;
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled      = false;
        imu.initialize(parameters);

        pidRotate = new PIDController(4, 0, 0);


        // Set up detector
        detector = new GoldAlignDetector(); // Create detector
        detector.init(hardwareMap.appContext, CameraViewDisplay.getInstance()); // Initialize it with the app context and camera
        detector.useDefaults(); // Set detector to use default settings

        // Optional tuning
        detector.alignSize = 100; // How wide (in pixels) is the range in which the gold object will be aligned. (Represented by green bars in the preview)
        detector.alignPosOffset = 0; // How far from center frame to offset this alignment zone.
        detector.downscale = 0.4; // How much to downscale the input frames

        detector.areaScoringMethod = DogeCV.AreaScoringMethod.MAX_AREA; // Can also be PERFECT_AREA
        //detector.perfectAreaScorer.perfectArea = 10000; // if using PERFECT_AREA scoring
        detector.maxAreaScorer.weight = 0.005; //

        detector.ratioScorer.weight = 5; //
        detector.ratioScorer.perfectRatio = 1.0; // Ratio adjustment

        detector.enable(); // Start the detector!

    }

    //This method converts the distance in inches to distance in number of tics
    public int getDistance(double inches)
    {
        return (int)(Math.floor(inches/INCHES_PER_TIC));
    }


    //Movement handles all driving for the robot during autonomous EXCEPT rotating
    public void movement(int distance, double power) {

        r_f_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        r_b_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        l_f_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        l_b_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        r_f_motor.setTargetPosition((int)(distance*wheelRatio));//wheel turns 1 time for every 18 motor turns
        r_b_motor.setTargetPosition((int)(distance*wheelRatio));
        l_f_motor.setTargetPosition((int)(distance*wheelRatio));
        l_b_motor.setTargetPosition((int)(distance*wheelRatio));

        r_f_motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        r_b_motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        l_f_motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        l_b_motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        drive(power);

        while (r_f_motor.isBusy() && r_b_motor.isBusy() && l_f_motor.isBusy() && l_b_motor.isBusy()){
            //Jokes on you kid there's nothing here get good
        }

        stopDriving();
        r_f_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        r_b_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        l_f_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        l_b_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

    }


    public void dump(){
        teamMarker.setPosition(.4); //THIS VALUE NEEDS FINDING
        sleep(3000);
    }



    private void resetAngle()
    {
        lastAngles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        globalAngle = 0;
    }

    /**
     * Get current cumulative angle rotation from last reset.
     * @return Angle in degrees. + = left, - = right.
     */
    private double getAngle()
    {
        // We experimentally determined the Z axis is the axis we want to use for heading angle.
        // We have to process the angle because the imu works in euler angles so the Z axis is
        // returned as 0 to +180 or 0 to -180 rolling back to -179 or +179 when rotation passes
        // 180 degrees. We detect this transition and track the total cumulative angle of rotation.

        Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        double deltaAngle = angles.firstAngle - lastAngles.firstAngle;

        if (deltaAngle < -180)
            deltaAngle += 360;
        else if (deltaAngle > 180)
            deltaAngle -= 360;

        globalAngle += deltaAngle;

        lastAngles = angles;

        return globalAngle;
    }

    /**
     * See if we are moving in a straight line and if not return a power correction value.
     * @return Power adjustment, + is adjust left - is adjust right.
     */
    private double checkDirection()
    {
        // The gain value determines how sensitive the correction is to direction changes.
        // You will have to experiment with your robot to get small smooth direction changes
        // to stay on a straight line.
        double correction, angle, gain = .10;

        angle = getAngle();

        if (angle == 0)
            correction = 0;             // no adjustment.
        else
            correction = -angle;        // reverse sign of angle for correction.

        correction = correction * gain;

        return correction;
    }

    /**
     * Rotate left or right the number of degrees. Does not support turning more than 180 degrees.
     * @param degrees Degrees to turn, - is right + is left
     */
    private void rotate(int degrees, double power)
    {
        double  leftPower, rightPower;

        // restart imu movement tracking.
        resetAngle();

        // getAngle() returns + when rotating counter clockwise (left) and - when rotating
        // clockwise (right).

        if (degrees < 0)
        {   // turn right.
            leftPower = power;
            rightPower = -power;
        }
        else if (degrees > 0)
        {   // turn left.
            leftPower = -power;
            rightPower = power;
        }
        else return;

        // set power to rotate.
        l_f_motor.setPower(leftPower);
        r_f_motor.setPower(leftPower);
        r_f_motor.setPower(rightPower);
        r_b_motor.setPower(rightPower);
        telemetry.addData("Assigned powers...", "");
        telemetry.update();
        sleep(1000);

        // rotate until turn is completed.
        if (degrees < 0)
        {
            // On right turn we have to get off zero first.
            while (opModeIsActive() && getAngle() == 0) {}
            telemetry.addData("Got off zero", "");
            telemetry.update();
            sleep(1000);
            while (opModeIsActive() && getAngle() > degrees) {}
        }
        else    // left turn.
        {

            while (opModeIsActive() && getAngle() < degrees) {}

        }


        // turn the motors off.
        telemetry.addData("Got out of loop", "");
        telemetry.update();
        stopDriving();

        // wait for rotation to stop.
        sleep(1000);

        // reset angle tracking on new heading.
        resetAngle();
    }



    //These methods handle moving to each location of a mineral during autonomous, as determined by the result of the tflow detector
    //BIG NOTE THIS ASSUMES WE HAVE ALREADY DETACHED FROM LANDER, WHICH IS NOT ACCURATE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    int angle = 20;
    double movPower = .15;

    public void right()
    {
        rotate(-angle,movPower);
        //rotateStart(true);
        //distance is actually about 38 inches, but we round to 40
        short distance = 4;
        //movement(getDistance(distance),movPower);
        rotate(angle,movPower);
        //rotateStart(false);
        movement(getDistance(distance),movPower);
        //dump();
        movement(getDistance(4),movPower);
        rotate(135,movPower);
        //rotateToCrater(true);
        movement(getDistance(50),movPower);
    }

    public void left()
    {
        rotate(angle,movPower);
        telemetry.addData("we cookin", "fire");
        telemetry.update();
        sleep(5000);
        //rotateStart(false);
        //distance is actually about 38 inches, but we round to 40
        short distance = 4;
        //movement(getDistance(distance),movPower);
        rotate(-angle,movPower);
        //rotateStart(true);
        //movement(getDistance(distance),movPower);
        //dump();
        //movement(getDistance(4),movPower);
        rotate(135,movPower);
        //rotateToCrater(false);
        //movement(getDistance(50),movPower);

    }
    public void center()
    {
        //distance is actually about 38 inches, but we round to 40
        short distance = 4;
        movement(getDistance(distance),movPower);
        //movement(getDistance(distance),movPower);
        // dump();
        movement(getDistance(4),movPower);
        rotate(135,movPower);  //use higher power for center rotation
        //rotateToCrater(true);
        movement(getDistance(50),movPower);
    }





    private static final String TFOD_MODEL_ASSET = "RoverRuckus.tflite";
    private static final String LABEL_GOLD_MINERAL = "Gold Mineral";
    private static final String LABEL_SILVER_MINERAL = "Silver Mineral";
    private VuforiaLocalizer vuforia;
    private TFObjectDetector tfod;


    public void stopDriving()
    {
        r_f_motor.setPower(0);
        r_b_motor.setPower(0);
        l_f_motor.setPower(0);
        l_b_motor.setPower(0);
    }
    public void drive(double power) {

        r_f_motor.setPower(power);
        r_b_motor.setPower(power);
        l_f_motor.setPower(power);
        l_b_motor.setPower(power);

    }
    public void getOut()
    {
        hang_motor.setPower(.5); //lower the robot
        sleep(10000);
        hang_motor.setPower(0);
        drive(.2);
        sleep(500);
        stopDriving();
        hang_motor.setPower(-.2); //lower the arm to normal length
        sleep(5000);
        hang_motor.setPower(0);
    }

    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = "AXJ2/Ef/////AAABmYg2ByAgSkUDjfWEgU69rPdaO9PZHapEjT+5RHgtQQXzE/fzBEJfZKnQ6Bv2beawKJZr60/cngSdKB8n8QwwnnB2F9KpoDgqdHo6Ya7mAhB3AtFh0Xe85qRPfKmd/4nYvQtlTFLMmCTv5wYhoNYfQx0g/N/zHBRV2f5s5hQi8dAcVzFgU8pBwP6g9T0jnSvhb3Ay/+RLt+VL3tgKF7hxpKnEr4gyrASxMXZkFi4uEe9/Va2MfreOL3u6+FEUoAS1HjKIrc/2i1ChdMu1sPtO6C0+ktvZB3SSrIVzMI0YVmE3DeN21bZkU4GUFf5B8YgkuljYu0syoxW20OySyE3ap4dmen3nq7svtrf4pTfGdvA+";
        parameters.cameraDirection = CameraDirection.BACK;

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Loading trackables is not necessary for the Tensor Flow Object Detection engine.

    }


    @Override

    public void runOpMode() {
        initialize();
        //Get Vuforia stuff ready
        sleep(5000);
        initVuforia();
        com.vuforia.CameraDevice.getInstance().setFlashTorchMode(true);

        //Detach from lander
        //getOut();

        telemetry.addData("IsAligned" , detector.getAligned()); // Is the bot aligned with the gold mineral?
        telemetry.addData("X Pos" , detector.getXPosition()); // Gold X position.
        double pos = detector.getXPosition();
        if (pos<200)
        {
            telemetry.addData("Pos: " , "left"); // Gold X position.
            telemetry.update();
            left();
        }
        else if (pos<400)
        {
            telemetry.addData("Pos: " , "middle"); // Gold X position.
            telemetry.update();
            center();
        }
        else if (pos>400)
        {
            telemetry.addData("Pos: " , "right"); // Gold X position.
            telemetry.update();
            right();
        }
        else{
            center();
        }





    }
}
