package gymtracker.domain

import org.scalatest.funsuite.AnyFunSuite

class TrainingDefaultsSpec extends AnyFunSuite:
  test("uses requested weekly session split"):
    assert(TrainingDefaults.targetsFor(WorkoutPreferences(3, 1, 45)) == List(MuscleGroup.Chest, MuscleGroup.Shoulders, MuscleGroup.Triceps))
    assert(TrainingDefaults.targetsFor(WorkoutPreferences(3, 2, 45)) == List(MuscleGroup.Quads, MuscleGroup.Hamstrings, MuscleGroup.Glutes, MuscleGroup.Calves))
    assert(TrainingDefaults.targetsFor(WorkoutPreferences(3, 3, 45)).contains(MuscleGroup.Back))

  test("two weekly sessions cover major muscle groups together"):
    val covered = (1 to 2).flatMap(session => TrainingDefaults.targetsFor(WorkoutPreferences(2, session, 45))).toSet
    assert(TrainingDefaults.majorMuscleGroups.forall(covered.contains))

  test("fitness level controls volume and rest"):
    val advanced = TrainingDefaults.defaultsFor(FitnessLevel.Advanced)
    assert(advanced.sets == 4)
    assert(advanced.reps.min == 6)
    assert(advanced.tempo == Tempo(3, 1, 2))
    assert(advanced.restSeconds == 120)
