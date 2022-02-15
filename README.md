# RaceAssist

[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B27464%2Fgithub.com%2FNlkomaru%2FRaceAssist-advance.svg?type=shield)](https://app.fossa.com/projects/custom%2B27464%2Fgithub.com%2FNlkomaru%2FRaceAssist-advance?ref=badge_shield)
[![Gradle Build Action](https://github.com/Nlkomaru/RaceAssist-advance/actions/workflows/blank.yml/badge.svg)](https://github.com/Nlkomaru/RaceAssist-advance/actions/workflows/blank.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

## structure
atanを使用してθを求めその差により順位決定をするためレース場は一周し交差のない形である必要があります。<br>
今後、交差させたレース場にも対応させるためベクトルによる算出に置き換える予定です。


## Commands

### Audience 
`/ra audience join <raceId>`  観客に自分を追加します  <br>
permission: `RaceAssist.commands.audience.join`<br>

`/ra audience leave <raceId>` 観客から自分を削除します  <br>
permission: `RaceAssist.commands.audience.leave`<br>

`/ra audience list <raceId>` 観客の一覧を表示  <br>
permission: `RaceAssist.commands.audience.list`<br>

### Bet  

`/ra bet can <raceId> on/off` 対象のレースに対して賭けが可能か変更します  <br>
permission: `RaceAssist.commands.bet.can`<br>

`/ra bet delete <raceId>`        賭けを削除します  <br>
permission: `RaceAssist.commands.bet.delete`<br>

`/ra bet list <raceId>`          賭けの一覧を表示します  <br>
permission: `RaceAssist.commands.bet.list`<br>

`/ra bet open <raceId>`          賭けをすることのできる画面を開くことができます  <br>
permission: `RaceAssist.commands.bet.open`<br>

`/ra bet rate <raceId>`          賭けのレートを変更します  <br>
permission: `RaceAssist.commands.bet.rate`<br>

`/ra bet revert <raceId>` すべての人に返金します <br>
permission: `RaceAssist.commands.bet.revert`<br>


`/ra bet remove <raceId> <betRow>` 指定した番号の賭けを返金 <br>
permission: `RaceAssist.commands.bet.remove`<br>

`/ra bet sheet <raceId> <SheetID>`       spreadsheetを登録します<br>
permission: `RaceAssist.commands.bet.sheet`<br>
`https://docs.google.com/spreadsheets/d/***********/edit#gid=0`  *****の部分をSheetIDに入力 

### Place 

`/ra place reverse <raceId>`  レースの走行方向の向きを反転  <br>
permission: `RaceAssist.commands.place.reverse`<br>

`/ra place central <raceId>`  レースの中心点を設定  <br>
permission: `RaceAssist.commands.place.central`<br>

`/ra place degree <raceId>`  レースのゴールの角度を設定(立っている場所基準90度刻み)  <br>
permission: `RaceAssist.commands.place.degree`<br>

`/ra place lap <raceId> <lap>`  レースのラップ数を指定  <br>
permission: `RaceAssist.commands.place.lap`<br>

`/ra place set <raceId> in|out` レース場の内周、外周を指定  <br>
permission: `RaceAssist.commands.place.set`<br>

`/ra place finish` 上記の設定の終了  <br>
permission: `RaceAssist.commands.place.finish`<br>

### Player  

`/ra player add <raceId> <Player>`  騎手を追加  <br>
permission: `RaceAssist.commands.player.add`<br>

`/ra player remove <raceId>`  騎手を削除  <br>
permission: `RaceAssist.commands.player.remove`<br>

`/ra player delete <raceId>`  騎手をすべて削除  <br>
permission: `RaceAssist.commands.player.delete`<br>

`/ra player list <raceId>`  騎手の一覧を表示  <br>
permission: `RaceAssist.commands.player.list`<br>

### Race 

`/ra race start <raceId>`  レースを開始  <br>
permission: `RaceAssist.commands.race.start`<br>

`/ra race debug <raceId>`  レースのデバッグ  <br>
permission: `RaceAssist.commands.race.debug`<br>

`/ra race stop <raceId>`  レースの停止  <br>
permission: `RaceAssist.commands.race.stop`<br>

`/ra race create <raceId>`  レースの作成  <br>
permission: `RaceAssist.commands.race.create`<br>

`/ra race delete <raceId>`  レースの削除  <br>
permission: `RaceAssist.commands.race.delete`<br>

`/ra race copy <raceId_1> <raceId_2>` レース1のsheetId、賭けリスト以外をすべてコピーします  <br>
permission: `RaceAssist.commands.race.copy`<br>

## permission

### 一般プレイヤー

`RaceAssist.commands.audience.join`
`RaceAssist.commands.audience.leave`
`RaceAssist.commands.bet.open`

### レース作成者

`RaceAssist.commands.*`

## Usage

### Race
creator :`/ra race create <raceId>`<br>
creator :`/ra place set <raceId> in`<br>
creator :`/ra place finish`<br>
creator :`/ra place set <raceId> out`<br>
creator :`/ra place finish`<br>
creator :`/ra place degree <raceId>`<br>
creator :`/ra place central <raceId>`<br>
creator :`/ra place reverse <raceId>` optional<br>
creator :`/ra player add <raceId> <Player>`<br>
audience :`/ra audience join <raceId>` <br>
creator :`/ra race start <raceId>`<br>

### Bet
creator :`/ra bet rate <raceId>`<br>
creator :`/ra bet sheet <raceId> <SheetID>` optional<br>
creator :`/ra bet can <raceId> on` <br>
player :`/ra bet open <raceId>`<br>
creator :`/ra bet can <raceId> off`<br>
race start<br>
creator :Pay manually<br>
creator :`/ra bet delete <raceId>`<br>

## About use sheets API

pluginフォルダの中のRaceAssistフォルダに**credentials.json**を入れます 最初の賭けが行われるとコンソールにOAuthの認証画面が開かれるのでスプレッドシートの所有者が認証してください

### credentials.jsonの設定

基本はAPIのダウンロードしたものにclient_secretを追加し、redirect_urlsのlocalhostを自分のドメイン(IP)に置き換える

```json
{
    "installed": {
        "client_id": "******.apps.googleusercontent.com",
        "project_id": "******",
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "token_uri": "https://oauth2.googleapis.com/token",
        "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
        "client_secret": "******",
        "redirect_uris": [
            "urn:ietf:wg:oauth:2.0:oob",
            "http://localhost"
        ]
    }
}
```

## translate 
[crowdin](https://crowdin.com/project/raceassist)<br>
`de_DE` : translate by DeepL API<br>
`en_US` : translate by DeepL API<br>
`fr_FR` : translate by DeepL API<br>
`he_IL`: translate by Google translate API<br>
`ja_JP` : Original <br>
`ko_KR`: translate by Google translate API<br>
`pt_PT` : translate by DeepL API<br>
`tok` : WIP<br>
`zh_CN` : translate by DeepL API<br>
`zh_TW`: translate by Google translate API<br>

## License

[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B27464%2Fgithub.com%2FNlkomaru%2FRaceAssist-advance.svg?type=large)](https://app.fossa.com/projects/custom%2B27464%2Fgithub.com%2FNlkomaru%2FRaceAssist-advance?ref=badge_large)
