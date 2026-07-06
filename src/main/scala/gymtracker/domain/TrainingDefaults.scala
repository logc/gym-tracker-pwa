package gymtracker.domain

object TrainingDefaults:
  val weeklySplit: Map[Weekday, List[MuscleGroup]] =
    Map(
      Weekday.Monday -> List(MuscleGroup.Chest, MuscleGroup.Shoulders, MuscleGroup.Triceps),
      Weekday.Tuesday -> List(MuscleGroup.Quads, MuscleGroup.Glutes, MuscleGroup.Hamstrings, MuscleGroup.Calves),
      Weekday.Wednesday -> List(MuscleGroup.Back, MuscleGroup.Biceps, MuscleGroup.Abs),
      Weekday.Thursday -> List(MuscleGroup.Chest, MuscleGroup.Shoulders, MuscleGroup.Triceps),
      Weekday.Friday -> List(MuscleGroup.Quads, MuscleGroup.Glutes, MuscleGroup.Hamstrings, MuscleGroup.Calves),
      Weekday.Saturday -> List(MuscleGroup.Back, MuscleGroup.Biceps, MuscleGroup.Abs),
      Weekday.Sunday -> List(MuscleGroup.Abs, MuscleGroup.LowerBack, MuscleGroup.Glutes)
    )

  val levelDefaults: Map[FitnessLevel, LevelDefaults] =
    Map(
      FitnessLevel.Beginner -> LevelDefaults(sets = 2, reps = RepRange(10, 12), tempo = Tempo(2, 0, 2), restSeconds = 75, exerciseCount = 4),
      FitnessLevel.Intermediate -> LevelDefaults(sets = 3, reps = RepRange(8, 12), tempo = Tempo(3, 0, 2), restSeconds = 90, exerciseCount = 5),
      FitnessLevel.Advanced -> LevelDefaults(sets = 4, reps = RepRange(6, 10), tempo = Tempo(3, 1, 2), restSeconds = 120, exerciseCount = 6)
    )

  def targetsFor(day: Weekday): List[MuscleGroup] =
    weeklySplit.getOrElse(day, weeklySplit(Weekday.Monday))

  def defaultsFor(level: FitnessLevel): LevelDefaults =
    levelDefaults(level)
