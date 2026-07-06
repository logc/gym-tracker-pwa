package gymtracker.domain

import org.scalatest.funsuite.AnyFunSuite

class WorkoutGeneratorSpec extends AnyFunSuite:
  private val machines =
    List(
      MachineClassifier.machineFromName("1", "Chest Press"),
      MachineClassifier.machineFromName("2", "Shoulder Press"),
      MachineClassifier.machineFromName("3", "Triceps Extension"),
      MachineClassifier.machineFromName("4", "Lat Pulldown"),
      MachineClassifier.machineFromName("5", "Leg Press"),
      MachineClassifier.machineFromName("6", "Leg Curl"),
      MachineClassifier.machineFromName("7", "Abdominal"),
      MachineClassifier.machineFromName("8", "Cable Machine")
    )

  test("generates selected weekly session without unknown machines"):
    val monday = WorkoutDate.fromYmd(2026, 6, 22)
    val workout = WorkoutGenerator.generate(machines, monday, FitnessLevel.Beginner, WorkoutPreferences(3, 1, 45), seed = 42)
    assert(workout.targetMuscles == List(MuscleGroup.Chest, MuscleGroup.Shoulders, MuscleGroup.Triceps))
    assert(workout.exercises.nonEmpty)
    assert(workout.exercises.forall(_.machineName != "Cable Machine"))
    assert(workout.exercises.filterNot(_.isTimed).forall(_.sets == 2))

  test("includes warm-up, cardio, and core blocks"):
    val monday = WorkoutDate.fromYmd(2026, 6, 22)
    val workout = WorkoutGenerator.generate(machines, monday, FitnessLevel.Intermediate, WorkoutPreferences(3, 2, 45), seed = 7)
    assert(workout.exercises.head.machineName == "Mobility and light stretching")
    assert(workout.exercises(1).machineName == "Easy cardio ramp-up")
    assert(workout.exercises.last.machineName == "Core finisher")
    assert(workout.exercises.count(_.isTimed) == 3)

  test("keeps generated estimate near target when enough machines are available"):
    val monday = WorkoutDate.fromYmd(2026, 6, 22)
    val workout = WorkoutGenerator.generate(machines, monday, FitnessLevel.Beginner, WorkoutPreferences(2, 1, 30), seed = 99)
    assert(workout.estimatedMinutes >= 27)
    assert(workout.estimatedMinutes <= 33)

  test("advanced routines use advanced defaults"):
    val monday = WorkoutDate.fromYmd(2026, 6, 22)
    val workout = WorkoutGenerator.generate(machines, monday, FitnessLevel.Advanced, WorkoutPreferences(3, 1, 45), seed = 42)
    val strength = workout.exercises.filterNot(_.isTimed)
    assert(strength.forall(_.sets == 4))
    assert(strength.forall(_.tempo == Tempo(3, 1, 2)))
    assert(strength.forall(_.restSeconds == 120))
