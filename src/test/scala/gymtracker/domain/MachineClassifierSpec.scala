package gymtracker.domain

import org.scalatest.funsuite.AnyFunSuite

class MachineClassifierSpec extends AnyFunSuite:
  test("classifies common chest press machines"):
    val result = MachineClassifier.classify("Plate Loaded Chest Press")
    assert(result.primaryMuscles.contains(MuscleGroup.Chest))
    assert(result.secondaryMuscles.contains(MuscleGroup.Triceps))
    assert(result.movementPattern == MovementPattern.HorizontalPush)

  test("classifies lat pulldown as vertical pull"):
    val result = MachineClassifier.classify("Lat Pulldown")
    assert(result.muscles.contains(MuscleGroup.Back))
    assert(result.muscles.contains(MuscleGroup.Biceps))
    assert(result.movementPattern == MovementPattern.VerticalPull)

  test("marks vague machines unknown"):
    val result = MachineClassifier.classify("Cable Machine")
    assert(result.isUnknown)
