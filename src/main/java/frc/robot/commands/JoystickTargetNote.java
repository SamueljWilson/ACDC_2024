package frc.robot.commands;

import java.util.function.Supplier;

import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.TargetConstants;
import frc.robot.PIDF;
import frc.robot.TunableDouble;
import frc.robot.TunablePIDF;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.Limelight;

public class JoystickTargetNote extends Command {
  // Also used by JoystickTargetSpeaker.
  public static TunableDouble targetAngularVelocityCoefficient =
    new TunableDouble("Target.angularVelocityCoefficient",
      TargetConstants.kAngularVelocityCoefficient);
  public static TunablePIDF targetTurningPIDF =
    new TunablePIDF("Target.turningPIDF", TargetConstants.kTurningPIDF);

  private final DriveSubsystem m_drive;
  private final Limelight m_limelight;
  private final Supplier<Double> m_xVelocitySupplier;
  private final Supplier<Double> m_yVelocitySupplier;

  private ProfiledPIDController m_thetaController = new ProfiledPIDController(
    targetTurningPIDF.get().p(),
    targetTurningPIDF.get().i(),
    targetTurningPIDF.get().d(),
    new TrapezoidProfile.Constraints(
      DriveConstants.kMaxAngularSpeedRadiansPerSecond,
      DriveConstants.kMaxAngularAccelerationRadiansPerSecondSquared));

  public JoystickTargetNote(DriveSubsystem drive, Limelight limelight,
      Supplier<Double> xVelocitySupplier, Supplier<Double> yVelocitySupplier) {
    m_drive = drive;
    m_limelight = limelight;
    m_xVelocitySupplier = xVelocitySupplier;
    m_yVelocitySupplier = yVelocitySupplier;
    addRequirements(drive, limelight);
  }

  @Override
  public void initialize() {
    m_thetaController.reset(0);
    m_thetaController.setTolerance(TargetConstants.kAngularTolerance);
    m_limelight.lightsOn();
  }

  @Override
  public void execute() {
    updateConstants();
    double x = m_limelight.getX();
    double thetaVelocity =
      m_thetaController.calculate(Units.degreesToRadians(x)) * targetAngularVelocityCoefficient.get();
    m_drive.drive(
      m_xVelocitySupplier.get(),
      m_yVelocitySupplier.get(),
      thetaVelocity,
      true
    );
  }

  private void updateConstants() {
    if (targetTurningPIDF.hasChanged()) {
      PIDF pidf = targetTurningPIDF.get();
      m_thetaController.setPID(pidf.p(), pidf.i(), pidf.d());
    }
  }

  @Override
  public void end(boolean isInterrupted) {
    m_limelight.lightsOff();
  }
}
