# BrickGame Kotlin - Improvements Plan

## Version 2.0 - Major Upgrade

### Phase 1: Core Mechanics ✅
- [x] Hold Piece - store/swap current piece, once per drop
- [x] Super Rotation System (SRS) - official wall kick tables
- [x] T-Spin Detection - Mini and Full with proper scoring
- [x] Lock Delay - 500ms grace period, 15-move reset limit
- [x] 7-Bag Randomizer - all 7 pieces in every bag
- [x] Next-3 Queue - preview 3 upcoming pieces
- [x] Hidden Rows - 4 rows above visible area (Guideline compliant)
- [x] Official Scoring - 100/300/500/800 x level
- [x] Back-to-Back Bonus - 1.5x for consecutive difficult clears
- [x] Combo Counter - 50 x combo x level bonus
- [x] Game Modes - Marathon, Sprint 40L, Ultra 2min
- [x] Counter-clockwise Rotation

### Phase 2: Architecture ✅
- [x] Lock delay loop (60fps separate coroutine)
- [x] DAS/ARR configurable input timing
- [x] Game mode support in ViewModel

### Phase 3: Landscape + UI ✅
- [x] Full landscape layout (3-zone: controls | board | next/rotate)
- [x] Automatic orientation detection
- [x] Hold piece preview (grayed when used)
- [x] Next-3 queue with size/alpha differentiation
- [x] Action labels (T-Spin, Tetris, B2B, Combo)
- [x] Sprint/Ultra timer display on game over
- [x] Manifest: removed portrait lock, added configChanges

### Phase 4: Layout System (Data Models) ✅
- [x] LayoutElement/LayoutProfile serializable models
- [x] LayoutPresets (Classic, Lefty, Compact)
- [x] LayoutRepository with DataStore persistence
- [x] Export/Import JSON support

### Future Improvements
- [ ] Drag-and-drop layout editor screen
- [ ] Unit tests for TetrisGame, SRS, ScoreCalculator
- [ ] UI tests for GameScreen
- [ ] Sound engine overhaul
- [ ] Accessibility (TalkBack, high contrast)
- [ ] Dependency upgrades (AGP 8.7+, Kotlin 2.1, Compose BOM 2025)
