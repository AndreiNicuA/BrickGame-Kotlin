# 🎮 Brick Game - Kotlin Native

A faithful recreation of the classic "Brick Game 9999 in 1" handheld console, built with Kotlin and Jetpack Compose for Android.

## ✨ Features

### 🎨 Multiple Themes
- **Classic** - Original green LCD look
- **Neon** - Cyberpunk cyan/magenta
- **Retro** - Warm orange/brown
- **Ocean** - Cool blue tones
- **Forest** - Natural green
- **Midnight** - Dark purple

### 🎯 Native Animations
- **Button Press** - Bounce and scale effects with haptic feedback
- **Piece Lock** - Flash animation when piece lands
- **Line Clear** - Multi-phase flash and collapse animation
- **Game Over** - Dramatic row-by-row collapse

### 📱 Optimized Controls
- Large, easy-to-press buttons
- D-Pad with proper labels
- Haptic feedback on every action
- Continuous movement on hold

### 💾 Persistence
- Theme preference saved
- High score tracking
- Vibration/sound settings

## 🏗️ Architecture

```
app/src/main/java/com/brickgame/tetris/
├── MainActivity.kt              # Entry point
├── game/
│   └── TetrisGame.kt           # Core game logic
├── ui/
│   ├── theme/
│   │   ├── GameTheme.kt        # 6 color themes
│   │   ├── Theme.kt            # Compose theme
│   │   └── Typography.kt       # Text styles
│   ├── animations/
│   │   ├── ButtonAnimations.kt # Press effects
│   │   └── GameAnimations.kt   # Line clear, etc.
│   ├── components/
│   │   ├── GameButton.kt       # D-Pad, Rotate
│   │   └── GameBoard.kt        # LCD display
│   └── screens/
│       ├── GameScreen.kt       # Main UI
│       ├── SettingsScreen.kt   # Settings
│       └── GameViewModel.kt    # State management
└── data/
    └── SettingsRepository.kt   # DataStore persistence
```

## 🚀 Getting Started

### Requirements
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Build

1. Clone the repository:
```bash
git clone https://github.com/AndreiNicuA/BrickGame-Kotlin.git
cd BrickGame-Kotlin
```

2. Open in Android Studio

3. Build and run:
```bash
./gradlew assembleDebug
```

### Install APK
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🎮 Controls

| Button | Action |
|--------|--------|
| ▲ (Up) | Hard Drop |
| ▼ (Down) | Soft Drop (hold) |
| ◀ (Left) | Move Left |
| ▶ (Right) | Move Right |
| ↻ (Rotate) | Rotate Piece |
| START | Start/Pause Game |
| RESET | Return to Menu |
| ON/OFF | Reset Game |
| SOUND | Toggle Sound |

## 📦 Dependencies

- **Jetpack Compose** - Modern UI toolkit
- **Material 3** - Design system
- **DataStore** - Preferences persistence
- **Lifecycle** - ViewModel & State
- **Coroutines** - Async operations

## 🎨 Theme Customization

Each theme defines:
- Device body color
- LCD screen colors (on/off pixels)
- Button colors (primary/secondary)
- Decoration colors (blue/red squares)
- Text colors

## 📱 Screenshots

[Add screenshots here]

## 📄 License

MIT License

## 👨‍💻 Author

**Andrei Anton**
- Email: andrei.nicu.anton@gmail.com
- GitHub: [@AndreiNicuA](https://github.com/AndreiNicuA)
