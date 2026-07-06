package gymtracker.domain

enum MuscleGroup:
  case Chest, Back, Shoulders, Biceps, Triceps, Quads, Hamstrings, Glutes, Calves, Abs, LowerBack, Adductors, Abductors

  def label: String =
    this match
      case Chest      => "chest"
      case Back       => "back"
      case Shoulders  => "shoulders"
      case Biceps     => "biceps"
      case Triceps    => "triceps"
      case Quads      => "quads"
      case Hamstrings => "hamstrings"
      case Glutes     => "glutes"
      case Calves     => "calves"
      case Abs        => "abs"
      case LowerBack  => "lower back"
      case Adductors  => "adductors"
      case Abductors  => "abductors"

object MuscleGroup:
  val all: List[MuscleGroup] = values.toList

  def fromLabel(value: String): Option[MuscleGroup] =
    all.find(_.label == value.trim.toLowerCase.replace("_", " "))

enum MovementPattern:
  case HorizontalPush, VerticalPush, HorizontalPull, VerticalPull, LowerPush, Hinge, KneeFlexion, HipExtension, Isolation, Core, Unknown

  def label: String =
    this match
      case HorizontalPush => "horizontal push"
      case VerticalPush   => "vertical push"
      case HorizontalPull => "horizontal pull"
      case VerticalPull   => "vertical pull"
      case LowerPush      => "lower push"
      case Hinge          => "hinge"
      case KneeFlexion    => "knee flexion"
      case HipExtension   => "hip extension"
      case Isolation      => "isolation"
      case Core           => "core"
      case Unknown        => "unknown"

object MovementPattern:
  val all: List[MovementPattern] = values.toList

  def fromLabel(value: String): Option[MovementPattern] =
    all.find(_.label == value.trim.toLowerCase.replace("_", " "))

enum FitnessLevel:
  case Beginner, Intermediate, Advanced

  def label: String = productPrefix.toLowerCase

object FitnessLevel:
  val all: List[FitnessLevel] = values.toList

  def fromLabel(value: String): Option[FitnessLevel] =
    all.find(_.label == value.trim.toLowerCase)

final case class Tempo(eccentric: Int, pause: Int, concentric: Int):
  override def toString: String = s"$eccentric-$pause-$concentric"

final case class RepRange(min: Int, max: Int)

final case class LevelDefaults(sets: Int, reps: RepRange, tempo: Tempo, restSeconds: Int, exerciseCount: Int)

final case class Machine(
    id: String,
    name: String,
    muscles: List[MuscleGroup],
    primaryMuscles: List[MuscleGroup],
    secondaryMuscles: List[MuscleGroup],
    movementPattern: MovementPattern,
    notes: Option[String] = None
):
  def isUnknown: Boolean =
    movementPattern == MovementPattern.Unknown || primaryMuscles.isEmpty

final case class MachineClassification(
    muscles: List[MuscleGroup],
    primaryMuscles: List[MuscleGroup],
    secondaryMuscles: List[MuscleGroup],
    movementPattern: MovementPattern,
    confidence: Double
):
  def isUnknown: Boolean =
    movementPattern == MovementPattern.Unknown || primaryMuscles.isEmpty

final case class PlannedExercise(
    id: String,
    machineId: String,
    machineName: String,
    muscles: List[MuscleGroup],
    movementPattern: MovementPattern,
    sets: Int,
    reps: Int,
    tempo: Tempo,
    restSeconds: Int
)

final case class PlannedWorkout(
    id: String,
    date: String,
    targetMuscles: List[MuscleGroup],
    fitnessLevel: FitnessLevel,
    exercises: List[PlannedExercise]
)

enum CueMode:
  case Beep, Voice, Silent

  def label: String = productPrefix.toLowerCase

object CueMode:
  val all: List[CueMode] = values.toList

  def fromLabel(value: String): Option[CueMode] =
    all.find(_.label == value.trim.toLowerCase)

enum Weekday:
  case Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday

  def label: String = productPrefix.toLowerCase

object Weekday:
  val all: List[Weekday] = values.toList

  def fromJsDay(value: Int): Weekday =
    value match
      case 0 => Sunday
      case 1 => Monday
      case 2 => Tuesday
      case 3 => Wednesday
      case 4 => Thursday
      case 5 => Friday
      case 6 => Saturday
      case _ => Monday

final case class WorkoutDate(iso: String, weekday: Weekday)

object WorkoutDate:
  def fromYmd(year: Int, month: Int, day: Int): WorkoutDate =
    val iso = f"$year%04d-$month%02d-$day%02d"
    WorkoutDate(iso, weekdayFor(year, month, day))

  // Sakamoto's algorithm avoids java.time, which Scala.js does not provide by default.
  private def weekdayFor(year: Int, month: Int, day: Int): Weekday =
    val offsets = Array(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val adjustedYear = if month < 3 then year - 1 else year
    val jsDay = (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets(month - 1) + day) % 7
    Weekday.fromJsDay(jsDay)
