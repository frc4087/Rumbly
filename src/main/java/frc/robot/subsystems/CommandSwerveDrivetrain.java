package frc.robot.subsystems;

import java.util.function.Supplier;

import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.mechanisms.swerve.SwerveDrivetrain;
import com.ctre.phoenix6.mechanisms.swerve.SwerveDrivetrainConstants;
import com.ctre.phoenix6.mechanisms.swerve.SwerveModuleConstants;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;//this was me
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Subsystem;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.generated.TunerConstants;
import frc.robot.generated.TunerConstants.Swerve.Mod0;
import frc.robot.generated.TunerConstants.Swerve.Mod1;
import frc.robot.generated.TunerConstants.Swerve.Mod2;
import frc.robot.generated.TunerConstants.Swerve.Mod3;

/**
 * Class that extends the Phoenix SwerveDrivetrain class and implements
 * subsystem so it can be used in command-based projects easily.
 */
public class CommandSwerveDrivetrain extends SwerveDrivetrain implements Subsystem {


    private static final double kSimLoopPeriod = 0.005; // 5 ms
    private Notifier m_simNotifier = null;
    private double m_lastSimTime;
    public SwerveDriveOdometry odometry;

    private static TalonFX mDriveMotor;

    /* Blue alliance sees forward as 0 degrees (toward red alliance wall) */
    private final Rotation2d BlueAlliancePerspectiveRotation = Rotation2d.fromDegrees(0);
    /* Red alliance sees forward as 180 degrees (toward blue alliance wall) */
    private final Rotation2d RedAlliancePerspectiveRotation = Rotation2d.fromDegrees(180);
    /* Keep track if we've ever applied the operator perspective before or not */
    private boolean hasAppliedOperatorPerspective = false;

    public CommandSwerveDrivetrain(SwerveDrivetrainConstants driveTrainConstants, double OdometryUpdateFrequency, SwerveModuleConstants... modules) {
        super(driveTrainConstants, OdometryUpdateFrequency, modules);
        if (Utils.isSimulation()) {
            startSimThread();
        }
    }
    public CommandSwerveDrivetrain(SwerveDrivetrainConstants driveTrainConstants, SwerveModuleConstants... modules) {
        super(driveTrainConstants, modules);
        if (Utils.isSimulation()) {
            startSimThread();
        }
    }

    public Command applyRequest(Supplier<SwerveRequest> requestSupplier) {
        return run(() -> this.setControl(requestSupplier.get()));
    }

    private void startSimThread() {
        m_lastSimTime = Utils.getCurrentTimeSeconds();

        /* Run simulation at a faster rate so PID gains behave more reasonably */
        m_simNotifier = new Notifier(() -> {
            final double currentTime = Utils.getCurrentTimeSeconds();
            double deltaTime = currentTime - m_lastSimTime;
            m_lastSimTime = currentTime;

            /* use the measured time delta, get battery voltage from WPILib */
            updateSimState(deltaTime, RobotController.getBatteryVoltage());
        });
        m_simNotifier.startPeriodic(kSimLoopPeriod);
    }

//     public class DriveSubsystem extends SubsystemBase {
//         public DriveSubsystem() {
//             AutoBuilder.configureHolonomic(
//             this::getPose, // Robot pose supplier
//             this::resetOdometry, // Method to reset odometry (will be called if your auto has a starting pose)
//             this::getRobotRelativeSpeeds, // ChassisSpeeds supplier. MUST BE ROBOT RELATIVE
//             this::driveRobotRelative, // Method that will drive the robot given ROBOT RELATIVE ChassisSpeeds
//             new HolonomicPathFollowerConfig( // HolonomicPathFollowerConfig, this should likely live in your Constants class
//                     new PIDConstants(3.0, 0.0, 0.0), // Translation PID constants
//                     new PIDConstants(100.0, 0.0, 0.2), // Rotation PID constants
//                     3, // Max module speed, in m/s
//                     0.47117, // Drive base radius in meters. Distance from robot center to furthest module.
//                     new ReplanningConfig() // Default path replanning config. See the API for the options here
//             ),
//             () -> {
//               // Boolean supplier that controls when the path will be mirrored for the red alliance
//               // This will flip the path being followed to the red side of the field.
//               // THE ORIGIN WILL REMAIN ON THE BLUE SIDE

//               var alliance = DriverStation.getAlliance();
//               if (alliance.isPresent()) {
//                 return alliance.get() == DriverStation.Alliance.Red;
//               }
//               return false;
//             },
//             this // Reference to this subsystem to set requirements
//     );

//   }

// //getPose method
// public Pose2d getPose() {
//     return odometry.getPoseMeters();
//   }

// public Pose2d getPose2d() {
//     return m_odometry.getEstimatedPosition();
//   }

//  //resetPose method 
// private final Pigeon2 m_gyro = new Pigeon2(10);

// public void resetEncoders() {
//     mDriveMotor.setPosition(0);
//   }

// public void resetModEncoders() {
//     Mod0.resetEncoders();
//     Mod1.resetEncoders();
//     Mod2.resetEncoders();
//     Mod3.resetEncoders();
//   }

// public void resetOdometry(Pose2d pose) {

//     resetModEncoders();

//     m_odometry.resetPosition(
//       m_gyro.getRotation2d(),
//         new SwerveModulePosition[] {
//             Mod0.getPosition(),
//             Mod1.getPosition(),
//             Mod2.getPosition(),
//             Mod3.getPosition()
//         },
//         pose);
//   }

//   //getRobotRelativeSpeeds method
//   public ChassisSpeeds getRobotRelativeSpeeds() {
//     return TunerConstants.Swerve.swerveKinematics.toChassisSpeeds(
//         Mod0.getState(),
//         Mod1.getState(),
//         Mod2.getState(),
//         Mod3.getState());
//   }

//   // driveRobotRelative method
//  public void driveRobotRelative(ChassisSpeeds chassisSpeeds) {
//     var swerveModuleStates = 
//         TunerConstants.Swerve.swerveKinematics.toSwerveModuleStates(chassisSpeeds);
  
//     SwerveDriveKinematics.desaturateWheelSpeeds(
//         (SwerveModuleState[]) swerveModuleStates, TunerConstants.kSpeedAt12VoltsMps);
  
//     Mod0.setDesiredState(swerveModuleStates[0], hasAppliedOperatorPerspective);
//     Mod1.setDesiredState(swerveModuleStates[1], hasAppliedOperatorPerspective);
//     Mod2.setDesiredState(swerveModuleStates[2], hasAppliedOperatorPerspective);
//     Mod3.setDesiredState(swerveModuleStates[3], hasAppliedOperatorPerspective);
//   }
// }
    
    @Override
    public void periodic() {
        /* Periodically try to apply the operator perspective */
        /* If we haven't applied the operator perspective before, then we should apply it regardless of DS state */
        /* This allows us to correct the perspective in case the robot code restarts mid-match */
        /* Otherwise, only check and apply the operator perspective if the DS is disabled */
        /* This ensures driving behavior doesn't change until an explicit disable event occurs during testing*/
        if (!hasAppliedOperatorPerspective || DriverStation.isDisabled()) {
            DriverStation.getAlliance().ifPresent((allianceColor) -> {
                this.setOperatorPerspectiveForward(
                        allianceColor == Alliance.Red ? RedAlliancePerspectiveRotation
                                : BlueAlliancePerspectiveRotation);
                hasAppliedOperatorPerspective = true;
            });
        }
    }
}