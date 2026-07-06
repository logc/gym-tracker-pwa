package gymtracker.domain

import scala.util.Random

object WorkoutGenerator:
  def generate(
      machines: List[Machine],
      date: WorkoutDate,
      fitnessLevel: FitnessLevel,
      preferences: WorkoutPreferences = WorkoutPreferences(3, 1, 45),
      seed: Long = System.currentTimeMillis()
  ): PlannedWorkout =
    val normalizedPreferences = preferences.normalized
    val targets = TrainingDefaults.targetsFor(normalizedPreferences)
    val defaults = TrainingDefaults.defaultsFor(fitnessLevel)
    val random = new Random(seed)
    val eligible = machines.filterNot(_.isUnknown)
    val matched = eligible.filter(machine => machine.muscles.exists(targets.contains))
    val pool = if matched.nonEmpty then matched else eligible
    val selected = fitToDuration(balancedSelection(pool, targets, pool.size, random), defaults, normalizedPreferences.targetMinutes)
    val strengthExercises =
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
    val exercises =
      warmUpBlock(date, normalizedPreferences) :: cardioBlock(date, normalizedPreferences) :: strengthExercises ::: List(coreBlock(date, normalizedPreferences))
    val estimate = estimateWorkoutMinutes(exercises, defaults)

    PlannedWorkout(
      id = s"${date.iso}-${fitnessLevel.label}-${normalizedPreferences.daysPerWeek}-${normalizedPreferences.sessionNumber}-${math.abs(seed)}",
      date = date.iso,
      targetMuscles = targets,
      fitnessLevel = fitnessLevel,
      exercises = exercises,
      preferences = normalizedPreferences,
      estimatedMinutes = estimate
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
        workout.copy(exercises = updated, estimatedMinutes = estimateWorkoutMinutes(updated, defaults))

  private def balancedSelection(pool: List[Machine], targets: List[MuscleGroup], count: Int, random: Random): List[Machine] =
    val shuffled = random.shuffle(pool)
    val byTarget =
      targets.flatMap(target => shuffled.find(_.muscles.contains(target)))
    val byPattern =
      shuffled.groupBy(_.movementPattern).values.flatMap(_.headOption).toList
    val combined = (byTarget ++ byPattern ++ shuffled).distinct
    combined.take(count)

  private def fitToDuration(selected: List[Machine], defaults: LevelDefaults, targetMinutes: Int): List[Machine] =
    val maxMinutes = math.ceil(targetMinutes * 1.1).toInt
    val fixedMinutes = warmUpMinutes(targetMinutes) + cardioMinutes(targetMinutes) + coreMinutes(targetMinutes)
    val perExercise = estimateStrengthMinutes(defaults)
    val allowedStrengthMinutes = (maxMinutes - fixedMinutes).max(perExercise)
    val allowedCount = (allowedStrengthMinutes / perExercise).max(1)
    selected.take(allowedCount)

  private def estimateWorkoutMinutes(exercises: List[PlannedExercise], defaults: LevelDefaults): Int =
    exercises.map {
      case exercise if exercise.isTimed => exercise.durationMinutes.getOrElse(0)
      case _                           => estimateStrengthMinutes(defaults)
    }.sum

  private def estimateStrengthMinutes(defaults: LevelDefaults): Int =
    val averageReps = (defaults.reps.min + defaults.reps.max) / 2.0
    val setSeconds = averageReps * (defaults.tempo.eccentric + defaults.tempo.pause + defaults.tempo.concentric)
    val restSeconds = defaults.restSeconds * (defaults.sets - 1).max(0)
    math.ceil((defaults.sets * setSeconds + restSeconds + 45) / 60.0).toInt.max(1)

  private def warmUpBlock(date: WorkoutDate, preferences: WorkoutPreferences): PlannedExercise =
    timedBlock(date, "warm-up", "Mobility and light stretching", List(MuscleGroup.Shoulders, MuscleGroup.Back, MuscleGroup.Quads, MuscleGroup.Hamstrings), MovementPattern.Unknown, warmUpMinutes(preferences.targetMinutes))

  private def cardioBlock(date: WorkoutDate, preferences: WorkoutPreferences): PlannedExercise =
    timedBlock(date, "cardio", "Easy cardio ramp-up", Nil, MovementPattern.Unknown, cardioMinutes(preferences.targetMinutes))

  private def coreBlock(date: WorkoutDate, preferences: WorkoutPreferences): PlannedExercise =
    timedBlock(date, "core", "Core finisher", List(MuscleGroup.Abs, MuscleGroup.LowerBack), MovementPattern.Core, coreMinutes(preferences.targetMinutes))

  private def timedBlock(date: WorkoutDate, id: String, name: String, muscles: List[MuscleGroup], pattern: MovementPattern, minutes: Int): PlannedExercise =
    PlannedExercise(
      id = s"${date.iso}-builtin-$id",
      machineId = s"builtin-$id",
      machineName = name,
      muscles = muscles,
      movementPattern = pattern,
      sets = 1,
      reps = 1,
      tempo = Tempo(minutes * 60, 0, 0),
      restSeconds = 0,
      durationMinutes = Some(minutes)
    )

  private def warmUpMinutes(targetMinutes: Int): Int =
    if targetMinutes <= 30 then 4 else 5

  private def cardioMinutes(targetMinutes: Int): Int =
    if targetMinutes <= 30 then 5 else if targetMinutes <= 45 then 7 else 8

  private def coreMinutes(targetMinutes: Int): Int =
    if targetMinutes <= 30 then 4 else 5
