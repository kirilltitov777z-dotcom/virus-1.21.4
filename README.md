# Virus Client (Minecraft 1.21.4, Fabric)

Клиент на базе открытого проекта [Silk](https://github.com/hitomdev/Silkcc) (MIT).
Собран под Minecraft **1.21.4** + Fabric Loader, Java 21.

## Что изменилось относительно Silk

- **Новый GUI** `cc.silk.gui.virus.VirusGui` в стиле скриншота:
  сайдбар с категориями (Бой / Движение / Игрок / Визуалы / Утилиты / Разное),
  поиск по русским и английским именам, «Включить все», две колонки карточек
  разной высоты (настройки больше не перекрывают соседние карточки), избранное
  (сердечко), переключатели, кликабельные слайдеры/чекбоксы/режимы и привязка
  клавиш по клику. Язык интерфейса переключается кнопкой `RU / EN` в шапке,
  выбор сохраняется в `virus/language.txt` в папке игры.
- **Гуи v3** (по новому скриншоту): полупрозрачное окно, сайдбар `All / Combat /
  Movement / Player / Renderer / Visual / Particles / Client`, поиск с быстрыми
  фильтрами (друзья / цель / избранное), сетка карточек в три колонки, описание
  каждой функции по наведению.
- **Новые модули:** `Fun Time`, `Spooky Time` (таймеры событий), `Particle Rain`,
  `Aura Particles`, `Custom FOV`.
- **Обходы для Aim Assist и Trigger Bot:** Anti-Detect, задержка смены цели,
  Only on Ground, Pause on Velocity, Reset on Death, GCD.
- **Kill Aura** — новый модуль с фильтрацией и сортировкой целей, GCD-ротацией,
  рейкаст-проверкой перед ударом и настройкой APS.
- **Aim Assist** — добавлены фиксация цели, приоритет по здоровью/дистанции,
  выравнивание по GCD.
- Открытие GUI — **Right Shift** (`NewClickGUIModule`).

## Сборка

Локально нужен JDK 21 и Gradle (обязательно reliance на wrapper):

```bash
./gradlew build
```

Готовый jar для установки: `build/libs/virus-1.21.4-1.0.0.jar` (не `-dev`).
Положить в `.minecraft/mods` вместе с Fabric API для 1.21.4.

## GitHub Actions

`.github/workflows/build.yml` при каждом push:
устанавливает JDK 21, собирает `./gradlew build`, загружает jar как артефакт,
а по тегу (`git tag v1.0.0 && git push --tags`) публикует Release.

## Лицензия

MIT. Проект использует код Silk (Copyright (c) 2025 koi) — см. `LICENSE`.
