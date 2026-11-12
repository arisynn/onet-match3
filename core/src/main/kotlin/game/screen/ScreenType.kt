package game.screen

/**
 * Enumeration of all 17 accessible screens within the Onet + Triple Match game.
 */
enum class ScreenType {
    BOOT,                     // 1. Splash screen
    LOADING,                  // 2. Loading screen
    MAIN_MENU,                // 3. Main menu
    MODE_SELECT,              // 4. Game mode selection
    ONET_GAME,                // 5. Onet gameplay
    TRIPLE_MATCH_GAME,        // 6. Triple Match gameplay
    LEVEL_SELECT,             // 7. Level selection
    VICTORY,                  // 8. Victory screen
    DEFEAT,                   // 9. Defeat screen
    PAUSE,                    // 10. Pause menu
    MISSION_CENTER,           // 11. Mission center
    ACHIEVEMENT_CENTER,       // 12. Achievement center
    PLAYER_PROFILE,           // 13. Player profile
    COSMETIC_COLLECTION,      // 14. Cosmetic collection
    COSMETIC_SHOP,            // 15. Cosmetic shop
    SETTINGS,                 // 16. Settings
    COSMETIC_PREVIEW,         // 17. Cosmetic preview & equip interface

    // Aliases for compatibility
    ROUND_RESULT,
    SHOP_COSMETICS,
    MISSIONS_ACHIEVEMENTS
}
