package gymtracker.domain

import org.scalatest.funsuite.AnyFunSuite

class WorkoutGeneratorSpec extends AnyFunSuite:
  private val machines =
    List(
      MachineClassifier.machineFromName("1", "Chest Press"),
      MachineClassifier.machineFromName("2", "Shoulder Press"),
      MachineClassifier.machineFromName("3", "Triceps Extension"),
      MachineClassifier.machineFromName("4", "Lat Pulldown"),
      MachineClassifier.machineFromName("5", "Cable Machine")
    )

  test("generates day-specific routine without unknown machines"):
    val monday = WorkoutDate.fromYmd(2026, 6, 22)
    val workout = WorkoutGenerator.generate(machines, monday, FitnessLevel.Beginner, seed = 42)
    assert(workout.targetMuscles == List(MuscleGroup.Chest, MuscleGroup.Shoulders, MuscleGroup.Triceps))
    assert(workout.exercises.nonEmpty)
    assert(workout.exercises.forall(_.machineName != "Cable Machine"))
    assert(workout.exercises.forall(_.sets == 2))

  test("advanced routines use advanced defaults"):
    val monday = WorkoutDate.fromYmd(2026, 6, 22)
    val workout = WorkoutGenerator.generate(machines, monday, FitnessLevel.Advanced, seed = 42)
    assert(workout.exercises.forall(_.sets == 4))
    assert(workout.exercises.forall(_.tempo == Tempo(3, 1, 2)))
    assert(workout.exercises.forall(_.restSeconds == 120))
