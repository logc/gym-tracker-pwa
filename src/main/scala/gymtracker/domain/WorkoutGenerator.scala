package gymtracker.domain

import scala.util.Random

object WorkoutGenerator:
  def generate(
      machines: List[Machine],
      date: WorkoutDate,
      fitnessLevel: FitnessLevel,
      seed: Long = System.currentTimeMillis()
  ): PlannedWorkout =
    val targets = TrainingDefaults.targetsFor(date.weekday)
    val defaults = TrainingDefaults.defaultsFor(fitnessLevel)
    val random = new Random(seed)
    val eligible = machines.filterNot(_.isUnknown)
    val matched = eligible.filter(machine => machine.muscles.exists(targets.contains))
    val pool = if matched.nonEmpty then matched else eligible
    val selected = balancedSelection(pool, targets, defaults.exerciseCount, random)
    val exercises =
      selected.zipWithIndex.map { case (machine, index) =>
        PlannedExercise(
          id = s"${date.iso}-${machine.id}-$index-${math.abs(random.nextInt())}",
          machineId = machine.id,
          machineName = machine.name,
          muscles = machine.muscles,
          movementPattern = machine.movementPattern,
          sets = defaults.sets,
          reps = random.between(defaults.reps.min, defaults.reps.max + 1),
          tempo = defaults.tempo,
          restSeconds = defaults.restSeconds
        )
      }

    PlannedWorkout(
      id = s"${date.iso}-${fitnessLevel.label}-${math.abs(seed)}",
      date = date.iso,
      targetMuscles = targets,
      fitnessLevel = fitnessLevel,
      exercises = exercises
    )

  def replaceExercise(
      workout: PlannedWorkout,
      exerciseId: String,
      machines: List[Machine],
      seed: Long = System.currentTimeMillis()
  ): PlannedWorkout =
    val usedIds = workout.exercises.filterNot(_.id == exerciseId).map(_.machineId).toSet
    val candidates =
      machines.filterNot(_.isUnknown).filterNot(machine => usedIds.contains(machine.id)).filter(machine => machine.muscles.exists(workout.targetMuscles.contains))
    val fallback = machines.filterNot(_.isUnknown).filterNot(machine => usedIds.contains(machine.id))
    val pool = if candidates.nonEmpty then candidates else fallback
    pool match
      case Nil => workout
      case nonEmpty =>
        val random = new Random(seed)
        val replacement = nonEmpty(random.nextInt(nonEmpty.size))
        val defaults = TrainingDefaults.defaultsFor(workout.fitnessLevel)
        val updated = workout.exercises.map {
          case exercise if exercise.id == exerciseId =>
            exercise.copy(
              id = s"${workout.date}-${replacement.id}-replacement-${math.abs(random.nextInt())}",
              machineId = replacement.id,
              machineName = replacement.name,
              muscles = replacement.muscles,
              movementPattern = replacement.movementPattern,
              reps = random.between(defaults.reps.min, defaults.reps.max + 1)
            )
          case exercise => exercise
        }
        workout.copy(exercises = updated)

  private def balancedSelection(pool: List[Machine], targets: List[MuscleGroup], count: Int, random: Random): List[Machine] =
    val shuffled = random.shuffle(pool)
    val byTarget =
      targets.flatMap(target => shuffled.find(_.muscles.contains(target)))
    val byPattern =
      shuffled.groupBy(_.movementPattern).values.flatMap(_.headOption).toList
    val combined = (byTarget ++ byPattern ++ shuffled).distinct
    combined.take(count)
