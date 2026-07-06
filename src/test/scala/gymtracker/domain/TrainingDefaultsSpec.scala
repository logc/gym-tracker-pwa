package gymtracker.domain

import org.scalatest.funsuite.AnyFunSuite

class TrainingDefaultsSpec extends AnyFunSuite:
  test("uses push muscles on Monday"):
    assert(TrainingDefaults.targetsFor(Weekday.Monday) == List(MuscleGroup.Chest, MuscleGroup.Shoulders, MuscleGroup.Triceps))

  test("fitness level controls volume and rest"):
    val advanced = TrainingDefaults.defaultsFor(FitnessLevel.Advanced)
    assert(advanced.sets == 4)
    assert(advanced.reps.min == 6)
    assert(advanced.tempo == Tempo(3, 1, 2))
    assert(advanced.restSeconds == 120)
