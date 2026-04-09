![Java Version](https://img.shields.io/badge/Java-21%2B-blue)
![License](https://img.shields.io/badge/License-Public%20Domain-blue)
![Build Status](https://github.com/anton-1488/orchid/actions/workflows/build.yml/badge.svg) <!-- Если добавишь CI -->

# Orchid 2.0 (Modern Tor Client for Java)

Это современный форк легендарного, но заброшенного проекта **Orchid**.
Я выкинул всё старое дерьмо, перевел проект на **Java 21**, **Maven** и **Виртуальные потоки (Project Loom)**.

## Почему этот форк?

Оригинальный Orchid застрял в 2014 году. Чтобы запустить его сегодня, тебе пришлось бы страдать. Я это исправил:

- 🚀 **Java 21**: Используем всю мощь современных фич.
- 🧵 **Virtual Threads**: Никаких тяжелых пулов потоков. Легкость и безумная производительность.
- 📦 **Maven**: Забудь про Ant и ручное скачивание JAR-ников. Просто добавь зависимость.
- 🧹 **No Guava**: Я вырезал тяжелую и старую Guava. Только чистая Java.
- 🔐 **Bouncy Castle 1.83+**: Актуальная криптография без дыр в безопасности.
- 🛠 **Zero Dependencies**: Максимально легкая либа.