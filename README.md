# RaceAssist-core

[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B27464%2Fgithub.com%2FNlkomaru%2FRaceAssist-advance.svg?type=shield)](https://app.fossa.com/projects/custom%2B27464%2Fgithub.com%2FNlkomaru%2FRaceAssist-advance?ref=badge_shield)
[![Gradle Build Action](https://github.com/Nlkomaru/RaceAssist-advance/actions/workflows/blank.yml/badge.svg)](https://github.com/Nlkomaru/RaceAssist-advance/actions/workflows/blank.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)


## Wiki

[Wiki](https://github.com/Nlkomaru/RaceAssist-core/wiki)

## structure
atanを使用してθを求めその差により順位決定をするためレース場は一周し交差のない形である必要があります。<br>
今後、交差させたレース場にも対応させるためベクトルによる算出に置き換える予定です。

## Recommended

レースごとに新しい名前のレースを作成することを必ずしてください<br>
copyコマンドを使用すると賭けられたデータ、sheetId、スタッフ、出場者を除きコピーすることができます

命名規則 : `<レースの名前>_<回数>`


## translate 
plugins/RaceAssist/Lang内に翻訳されたpropertiesファイルを入れるとその言語を使用している場合はその言語が表示されます。 <br>
標準ではja_JPを使用します。

[crowdin](https://crowdin.com/project/raceassist)<br>


## License

[GPLv3ライセンス](https://github.com/Nlkomaru/RaceAssist-core/blob/master/LICENSE)での公開です。ソースコードの使用規約等はGPLv3ライセンスに従います。

[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B27464%2Fgithub.com%2FNlkomaru%2FRaceAssist-core.svg?type=large)](https://app.fossa.com/projects/custom%2B27464%2Fgithub.com%2FNlkomaru%2FRaceAssist-core?ref=badge_large)
