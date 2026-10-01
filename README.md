
## 制作背景
家計簿をつけるにあたって支出の入力の手間が課題となっていた。

そのためLINE上で支出をつけるようにすることによって入力を継続しやすくなると考えた。

## 技術選定
| 技術 | 選定理由 |
|:--| :-- |
|LINE Message API | 入力の手間を省き、継続率を上げるため |
|Lambda| 入力のタイミングでサーバーが起動するLambdaはシステムを使用する時間が少ない家計簿アプリに適切であるから|
|RDS MySQL|支出の合計を出力する必要があったため|
|Secrets Manager| データベースのパスワードを安全に扱うため |



## アーキテクチャ図
ユーザーの支出を保存
```mermaid
  flowchart LR
   LINE["LINE Message API"] --> API["API Gateway"]
   API-->Lambda["AWS Lambda"]
   Lambda-->RDS["RDS MySQL"]
```
グラフを出力
```mermaid
  flowchart LR
  LINE["LINE Message API"] --> API["API Gateway"]
  API-->Lambda["AWS Lambda"]
  Lambda-->RDS["RDS MySQL"]
  RDS-->Lambda
  Lambda--> LINE
```
## 使用方法
ユーザーがLINEで[1200 食費 メモ]のように送ると
DBに1200,食費が保存される
ユーザーが[グラフ]と送るとDBの情報を集計し、テキストで日ごとの使用金額がラインで送られてくる
## DBの設計
```mermaid
erDiagram
 EXPENSE{
  BIGINT id PK "主キー"
  VARCHAR user_id "LINE User ID"
  INT amount "金額"
  VARCHAR category "カテゴリ(食費など)"
  TEXT memo "メモ"
  DATETIME created_at "登録日時"
 }
```
ユーザごとにデータを分離している
## Lambdaの機能
### DailyTotal.java:[日付、その日の支出の合計金額]を1セットにまとめるデータ型を定義

### DbAccess.java:SQLにアクセスし、支出情報をINSERTする

### DbConfig.java:データベース接続に必要な情報を1つにまとめて管理する

### GetStatus.java:SQLから日別の支出合計を取得する

### Handler.java:LINEのメッセージをJSONから取り出し、メインの処理を行う。
### JdbcUtil.java:JavaオブジェクトからJDBC用のURLを作る
### SecretsUtil.java:AWS Secrets Managerから秘密情報(DBのパスワードなど)を取り出してくる
### FormatStatus.java:フォーマットの情報を保持
### FormatCheck.java:フォーマットがあっているか確認
## Lambdaのメイン処理
1.API GatewayからのWebhook JSONを受け取る
2.Messageからtextを取り出す
3.入力形式を判定
  [数字、カテゴリ、メモ]ならSQLにINSERT
  [グラフ]ならSQLからSELECTし、集計したデータをLINEで返す
## セキュリティ
DBパスワードをAWS Secrets Managerに保存
Lambdaには環境変数のみを記述
## 今後の改善案
ユーザーからの入力の型をより自由にする
入力の取り消しを可能にする
月ごとの合計金額や、カテゴリーごとの合計金額を出力する
過去にさかのぼって支出の入力、取り消しを可能にするためにDBに新しいカラムを追加する
