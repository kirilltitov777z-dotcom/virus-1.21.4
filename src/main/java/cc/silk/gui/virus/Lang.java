package cc.silk.gui.virus;

import cc.silk.SilkClient;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/** UI/module localisation for the Virus click GUI. Choice is kept between launches. */
public final class Lang {

    public enum Mode { RU, EN }

    private static Mode mode = Mode.RU;
    private static boolean loaded;

    private static final Map<String, String> UI = Map.ofEntries(
            Map.entry("search", "Поиск модулей..."),
            Map.entry("enableAll", "Включить все"),
            Map.entry("favorites", "Избранное"),
            Map.entry("friends", "Друзья"),
            Map.entry("configs", "Конфиги"),
            Map.entry("interface", "Интерфейс"),
            Map.entry("macros", "Макросы"),
            Map.entry("misc", "Разное"),
            Map.entry("combat", "Бой"),
            Map.entry("movement", "Движение"),
            Map.entry("player", "Игрок"),
            Map.entry("visual", "Визуалы"),
            Map.entry("all", "Все"),
            Map.entry("renderer", "Рендер"),
            Map.entry("particles", "Частицы"),
            Map.entry("hint", "Введите ваш поисковый запрос..."),
            Map.entry("admin", "Администратор"),
            Map.entry("key", "Клавиша"),
            Map.entry("listening", "..."),
            Map.entry("noModules", "Нет модулей")
    );

    private static final Map<String, String> MODULES = Map.ofEntries(
            Map.entry("2D ESP", "2D ESP"),
            Map.entry("Aim Assist", "Помощь в наведении"),
            Map.entry("Anti Miss", "Анти-промах"),
            Map.entry("ArrayList", "Список модулей"),
            Map.entry("Arrow ESP", "Подсветка стрел"),
            Map.entry("Auto Cart", "Авто-вагонетка"),
            Map.entry("Auto Crafter", "Авто-крафт"),
            Map.entry("Auto Crystal", "Авто-кристаллы"),
            Map.entry("Auto Double Hand", "Авто-вторая рука"),
            Map.entry("Auto Drain", "Авто-слив"),
            Map.entry("Auto Extinguish", "Авто-тушение"),
            Map.entry("Auto Firework", "Авто-фейерверк"),
            Map.entry("Auto Head Hitter", "Авто-удар по голове"),
            Map.entry("Auto MLG", "Авто-MLG"),
            Map.entry("Auto Mace", "Авто-дубина"),
            Map.entry("Auto Pot", "Авто-зелья"),
            Map.entry("Auto Refill", "Авто-пополнение"),
            Map.entry("Auto Tool", "Авто-инструмент"),
            Map.entry("Auto Web", "Авто-паутина"),
            Map.entry("Breach Swap", "Замена при пробое"),
            Map.entry("Cart Key", "Клавиша вагонетки"),
            Map.entry("Click Gui", "Клик-гуи"),
            Map.entry("Client", "Клиент"),
            Map.entry("ClientSettings", "Настройки клиента"),
            Map.entry("Container Slots", "Слоты контейнера"),
            Map.entry("Cover Up", "Скрытие"),
            Map.entry("Criticals", "Криты"),
            Map.entry("Crystal Optimizer", "Оптимизация кристаллов"),
            Map.entry("Debugger", "Отладчик"),
            Map.entry("Elytra HotSwap", "Быстрая смена элитр"),
            Map.entry("Fake Player", "Фейковый игрок"),
            Map.entry("Fast Exp", "Быстрый опыт"),
            Map.entry("Fast Mine", "Быстрая добыча"),
            Map.entry("Fast Place", "Быстрая установка"),
            Map.entry("Fun Time", "Фан-тайм"),
            Map.entry("Spooky Time", "Спуки-тайм"),
            Map.entry("Particle Rain", "Дождь частиц"),
            Map.entry("Aura Particles", "Аура частиц"),
            Map.entry("Custom FOV", "Свой обзор"),
            Map.entry("Friends", "Друзья"),
            Map.entry("Full Bright", "Полная яркость"),
            Map.entry("Hit Cob", "Удар паутиной"),
            Map.entry("Hover Totem", "Тотем наведённого"),
            Map.entry("Keep Sprint", "Сохранять спринт"),
            Map.entry("Key Anchor", "Клавиша якоря"),
            Map.entry("Key Crystal", "Клавиша кристалла"),
            Map.entry("Key Lava", "Клавиша лавы"),
            Map.entry("Keybinds", "Привязки клавиш"),
            Map.entry("Kill Aura", "Килл-аура"),
            Map.entry("Middle Click Friend", "Друг по средней кнопке"),
            Map.entry("NewClickGUI", "Новый клик-гуи"),
            Map.entry("Notifications", "Уведомления"),
            Map.entry("Outline ESP", "Контур ESP"),
            Map.entry("Pearl Catch", "Ловля жемчуга"),
            Map.entry("Pearl Key", "Клавиша жемчуга"),
            Map.entry("Ping Spoof", "Фейк пинга"),
            Map.entry("ReBuff Notifier", "Уведомление о баффе"),
            Map.entry("STap", "S-тап"),
            Map.entry("Secret", "Секрет"),
            Map.entry("Shield Breaker", "Ломатель щита"),
            Map.entry("Sprint", "Спринт"),
            Map.entry("Stun Cob", "Оглушение паутиной"),
            Map.entry("Swing Speed", "Скорость взмаха"),
            Map.entry("Sword Hotswap", "Быстрая смена меча"),
            Map.entry("Sword Swap", "Замена меча"),
            Map.entry("Target ESP", "ESP цели"),
            Map.entry("Target HUD", "HUD цели"),
            Map.entry("Teams", "Команды"),
            Map.entry("Throw Pot", "Бросок зелья"),
            Map.entry("Totem Hit", "Удар тотемом"),
            Map.entry("Trajectories", "Траектории"),
            Map.entry("Trap Save", "Спасение из ловушки"),
            Map.entry("Trigger Bot", "Триггер-бот"),
            Map.entry("Velocity", "Велосити"),
            Map.entry("WTap", "W-тап"),
            Map.entry("Watermark", "Водяной знак"),
            Map.entry("Wind Charge Key", "Клавиша ветряного заряда")
    );

    private static final Map<String, String> WORDS = Map.ofEntries(
            Map.entry("Attack", "Атака"), Map.entry("Range", "Дальность"), Map.entry("Aim", "Наведение"),
            Map.entry("Speed", "Скорость"), Map.entry("Delay", "Задержка"), Map.entry("Rotation", "Поворот"),
            Map.entry("Priority", "Приоритет"), Map.entry("Health", "Здоровье"), Map.entry("Distance", "Дистанция"),
            Map.entry("Players", "Игроки"), Map.entry("Mobs", "Мобы"), Map.entry("Animals", "Животные"),
            Map.entry("Invisible", "Невидимые"), Map.entry("Walls", "Стены"), Map.entry("Through", "Сквозь"),
            Map.entry("Crits", "Криты"), Map.entry("Critical", "Крит"), Map.entry("Shield", "Щит"),
            Map.entry("Break", "Ломать"), Map.entry("Focus", "Фокус"), Map.entry("Target", "Цель"),
            Map.entry("Correction", "Коррекция"), Map.entry("Movement", "Движение"), Map.entry("Only", "Только"),
            Map.entry("Color", "Цвет"), Map.entry("Mode", "Режим"), Map.entry("Key", "Клавиша"),
            Map.entry("Toggle", "Переключение"), Map.entry("Time", "Время"), Map.entry("Times", "Раз"),
            Map.entry("Chance", "Шанс"), Map.entry("Random", "Случайно"), Map.entry("Min", "Мин"),
            Map.entry("Max", "Макс"), Map.entry("Threshold", "Порог"), Map.entry("Radius", "Радиус"),
            Map.entry("Size", "Размер"), Map.entry("Width", "Ширина"), Map.entry("Height", "Высота"),
            Map.entry("Alpha", "Прозрачность"), Map.entry("Fill", "Заливка"), Map.entry("Line", "Линия"),
            Map.entry("Outline", "Контур"), Map.entry("Glow", "Свечение"), Map.entry("Gradient", "Градиент"),
            Map.entry("Background", "Фон"), Map.entry("Text", "Текст"), Map.entry("Font", "Шрифт"),
            Map.entry("Animation", "Анимация"), Map.entry("Smoothing", "Сглаживание"),
            Map.entry("FOV", "Угол обзора"), Map.entry("Switch", "Переключение"),
            Map.entry("Back", "Назад"), Map.entry("Slot", "Слот"), Map.entry("Inventory", "Инвентарь"),
            Map.entry("Offhand", "Левая рука"), Map.entry("Sword", "Меч"), Map.entry("Axe", "Топор"),
            Map.entry("Axes", "Топоры"), Map.entry("Swords", "Мечи"), Map.entry("Bow", "Лук"),
            Map.entry("Auto", "Авто"), Map.entry("Weapon", "Оружие"),
            Map.entry("Weapons", "Оружие"), Map.entry("Blocks", "Блоки"), Map.entry("Block", "Блок"),
            Map.entry("Ignore", "Игнорировать"), Map.entry("Show", "Показывать"), Map.entry("Hide", "Скрывать"),
            Map.entry("Self", "Себя"), Map.entry("Friends", "Друзья"), Map.entry("Team", "Команда"),
            Map.entry("Ground", "Земля"), Map.entry("Air", "Воздух"), Map.entry("Fall", "Падение"),
            Map.entry("Damage", "Урон"), Map.entry("Armor", "Броня"), Map.entry("Totem", "Тотем"),
            Map.entry("Crystal", "Кристалл"), Map.entry("Pearl", "Жемчуг"), Map.entry("Pot", "Зелье"),
            Map.entry("Web", "Паутина"), Map.entry("Lava", "Лава"), Map.entry("Anchor", "Якорь"),
            Map.entry("Notification", "Уведомление"), Map.entry("Sound", "Звук"), Map.entry("Volume", "Громкость"),
            Map.entry("Position", "Позиция"), Map.entry("Offset", "Смещение"), Map.entry("Spacing", "Отступ"),
            Map.entry("Padding", "Отступ"), Map.entry("Sort", "Сортировка"), Map.entry("Strength", "Сила"),
            Map.entry("Variation", "Вариация"), Map.entry("Shape", "Форма"),
            Map.entry("Profile", "Профиль"), Map.entry("Enforce", "Применять"), Map.entry("Limits", "Ограничения"),
            Map.entry("Event", "Событие"), Map.entry("Interval", "Интервал"),
            Map.entry("CPS", "Клики"), Map.entry("Click", "Клик"),
            Map.entry("Hold", "Удерживать"), Map.entry("Sneak", "Приседание"), Map.entry("Fly", "Полёт"),
            Map.entry("Space", "Пробел"), Map.entry("Jump", "Прыжок"), Map.entry("Sprint", "Спринт"),
            Map.entry("Container", "Контейнер"), Map.entry("Chest", "Сундук"),
            Map.entry("Ms", "мс"), Map.entry("MS", "мс"),
            Map.entry("Per", "за"), Map.entry("Second", "секунду"), Map.entry("Seconds", "секунд"),
            Map.entry("When", "Когда"), Map.entry("Using", "Использовании"),
            Map.entry("Item", "Предмет"), Map.entry("On", "Вкл"), Map.entry("Off", "Выкл")
    );

    private Lang() {
    }

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File dir = SilkClient.mc != null ? SilkClient.mc.runDirectory : null;
            File file = dir != null ? new File(dir, "virus") : null;
            if (file != null && file.isDirectory()) {
                File lang = new File(file, "language.txt");
                if (lang.isFile()) {
                    String value = new String(Files.readAllBytes(lang.toPath()), StandardCharsets.UTF_8).trim();
                    if ("en".equalsIgnoreCase(value)) mode = Mode.EN;
                    if ("ru".equalsIgnoreCase(value)) mode = Mode.RU;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static Mode mode() {
        ensureLoaded();
        return mode;
    }

    public static boolean russian() {
        return mode() == Mode.RU;
    }

    public static void toggle() {
        ensureLoaded();
        mode = mode == Mode.RU ? Mode.EN : Mode.RU;
        try {
            File file = new File(SilkClient.mc.runDirectory, "virus");
            if (!file.exists()) file.mkdirs();
            Files.write(new File(file, "language.txt").toPath(),
                    (mode == Mode.RU ? "ru" : "en").getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public static String ui(String key) {
        ensureLoaded();
        if (mode == Mode.EN) {
            return switch (key) {
                case "search" -> "Search modules...";
                case "enableAll" -> "Enable all";
                case "favorites" -> "Favorites";
                case "friends" -> "Friends";
                case "configs" -> "Configs";
                case "interface" -> "Interface";
                case "macros" -> "Macros";
                case "misc" -> "Various";
                case "combat" -> "Combat";
                case "movement" -> "Movement";
                case "player" -> "Player";
                case "visual" -> "Visuals";
                case "utilities" -> "Utilities";
                case "all" -> "All";
                case "renderer" -> "Renderer";
                case "particles" -> "Particles";
                case "hint" -> "Enter your search query...";
                case "admin" -> "Administrator";
                case "key" -> "Keybind";
                case "listening" -> "...";
                case "noModules" -> "No modules";
                default -> key;
            };
        }
        return UI.getOrDefault(key, key);
    }

    public static String module(String name) {
        ensureLoaded();
        return mode == Mode.RU ? MODULES.getOrDefault(name, name) : name;
    }

    public static String setting(String name) {
        ensureLoaded();
        if (mode != Mode.RU) return name;
        String[] parts = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(' ');
            String word = parts[i];
            String translated = WORDS.get(word);
            result.append(translated != null ? translated : word);
        }
        return result.toString();
    }
}
