# Voxelgame workshop: Kliendi-Serveri Autoriteet, Protseduuriline Graafika ja AI

Tegemist on 3D Minecraft-stiilis mängu projektiga, milles on osad funktsionaalsused katki või eemaldatud. Sinu ülesanne on need koodi tagasi lisada, parandada serveri turvalisust ning luua tehisintellekti agendi abil uusi visuaalseid lahendusi.

**NB! Palun tutvu enne töötoaga alustamist esialgse projektiga ja selle README-ga, et teaksid, kuidas mäng töötama peaks.**

## Kontseptid ja Tööriistad

*   **[libGDX wiki](https://libgdx.com/wiki/)**: Raamistik, millele antud mäng toetub.


*   **[OpenGL tutorial](https://learnopengl.com/getting-started/shaders)**: Mäng kasutab OpenGL-i varjutajaid (shaders) graafika renderdamiseks. See tutorial on suurepärane sissejuhatus GLSL-i ja varjutajate loomiseks.


*   **Võrguprogrammeerimise kuldreegel**: *Server on autoriteet.* Klient on ainult "rumal" kuvaja, mis edastab kasutaja soove. Server peab alati valideerima, kas kasutaja tegevus on tegelikult lubatud.


*   **Protseduuriline genereerimine**: Selles projektis pole kasutatud ühtegi pildifaili (`.png`) ega 3D mudelit (`.obj`, `.gltf`). Nii maailm, tekstuurid kui ka mängija mudel luuakse reaalajas koodi (matemaatika ja müra algoritmide) abil. Uuri klasse `PlayerModelGenerator`, `TerrainGenerator` ja `Shaders.java`.


### 🤖 Koodibaasiga tutvumine ja AI (*Agentic Coding*)
Kuna tegemist on keskmisest suurema projektiga (sisaldab füüsikat, võrguliiklust, OpenGL varjutajaid ehk *shadereid* ja polügoonide genereerimist), soovitame koodibaasiga tutvumiseks ja koodi kirjutamiseks kasutada **tehisintellekti tööriistu** (nt IDEA Copilot plugin, Google Antigravity või OpenAI Codex).

Tänapäeval on standardiks **agentic coding** – arendusviis, kus AI aitab sul iseseisvalt koodibaasis navigeerida.
*   **Kuidas küsida?** *"Palun selgita, kuidas liigub info klahvivajutusest kliendis (PlayerInputManager) serveri füüsikamootorini."* või *"Kus failis võetakse serveris vastu BlockChangeMessage ja kuidas ma saan sinna mängija koordinaadid juurde panna?"*
*   **TÄHELEPANU!** AI tööriistad pakuvad tihti kõige laiskemat lahendust. Võrguarhitektuuri osas võib AI teha kriitilisi vigu (näiteks pakkuda valideerimist ainult kliendi poolel). Arhitektuuri ja turvalisuse eest vastutad sina!

---

## Eemaldatud ja katkised funktsionaalsused

**NB!** Enne ja pärast iga funktsionaalsuse lisamist pane projekt tööle. Kaitsmisel arvestatakse ainult korrektselt töötavate lahendustega!

### 1. "New Game" või "Join Game" nupule vajutades avaneb mäng
Hetkel vajutades menüüs nuppe, mäng ei alga – ekraan jääb menüüsse, kuigi taustal serveriga ühendus luuakse.

<details> 
<summary>💡 Vihje 1</summary> 

Muudatus tuleb teha **kliendi klassis** `TitleScreen`. Nuppude defineerimisel (`continueButton` ja `newGameButton`) saadetakse serverile sõnum, aga ekraani ennast ei vahetata.
</details> 


<details> 
<summary>💡 Vihje 2</summary> 

Kliendi ekraanide vahetamiseks kasutatakse `game.setScreen(...)` meetodit, kus `game` on `VoxelGame` klassi instants. Sinu ülesanne on leida õige koht, kuhu see lisada, nii et peale nupuvajutust vahetuks ekraan 3D mängu vaate vastu.
</details> 


<details> 
<summary>💡 Vihje 3</summary> 

Vajaliku ekraani klass on `VoxelScreen`, mis asub paketis `screen`. Selle konstruktorisse tuleb edastada `game` objekt.
</details> 


<details> 
<summary>🛠 Lahendus</summary> 

Mõlema nupu lambda-avaldise lõppu tuleb lisada ekraani vahetus 3D vaate vastu:
```java 
game.setScreen(new VoxelScreen(game)); 
``` 
</details>

### 2. Gravitatsioon on katki (Mängija ei saa hüpata)
Tühikut vajutades tegelane ei hüppa. Konstandid on katki läinud.

<details> 
<summary>💡 Vihje 1</summary> 

Kõik füüsika ja mängija liikumisega seotud numbrid on defineeritud ühes kindlas klassis `constant` paketis. (Proovi küsida AI käest: *"Kus on defineeritud füüsika konstandid?"*)
</details>

<details> 
<summary>🛠 Lahendus</summary> 

Ava klass `Constants` ja paranda muutuja `JUMP_VELOCITY` funktsionaalseks:
```java 
public static final float JUMP_VELOCITY = 10f; 
``` 
</details>

### 3. "Lendava häkkeri" peatamine (Server-side validation)
Praegu saab klient vajutada 'F' klahvi ning saata serverile sõnumi `fly = true`. Server usaldab seda ja lubab lennata! Sinu ülesanne on muuta serveri loogikat nii, et lendamine poleks lubatud (ignoreeri kliendi `fly` väärtust).

<details> 
<summary>💡 Vihje 1</summary>

Kliendi sisend jõuab serverisse sõnumina `PlayerMovementMessage`, millel on väli `fly`. Serveri poolel tuleb leida koht, kus seda sõnumit töödeldakse, ja veenduda, et `fly` väärtust ei kasutata mängija liikumise mõjutamiseks.
</details>

<details> 
<summary>💡 Vihje 2</summary>

Kliendi sisend jõuab serverisse läbi klassi `PlayerMovementListener`, mis kutsub välja `Player` klassi meetodi `handleInput(PlayerMovementMessage message)`.
</details>

<details> 
<summary>🛠 Lahendus</summary> 

Muuda `Player.java` meetodit `handleInput` nii, et lendamise *boolean* seadistatakse alati väärtusele `false`.
```java
// Kliendi "fly" soovi ignoreeritakse, server on autoriteet!
inputState.setFly(false); 
```
</details>

### 4. Pika käega ehitaja (Kauguse valideerimine)
Häkkerist klient võiks saata `BlockChangeMessage` sõnumi, et ta ehitab ploki 1000 ühikut eemale. Server peab kontrollima, kas mängija asub ehitatavast plokist piisavalt lähedal (näiteks maksimaalselt 6 ühiku kaugusel).

<details> 
<summary>💡 Vihje 1</summary> 

Serveris tegeleb plokkide muutmisega `GameInstance.handleBlockChange(Connection connection, int x, int y, int z, int blockType)`.
</details>

<details> 
<summary>🛠 Lahendus</summary> 

Leia `players` listist õige mängija, arvuta 3D distants mängija koordinaatide (`physicsState`) ja ploki koordinaatide vahel (`Math.sqrt(...)`) ning ignoreeri ehitust (tee `return;`), kui vahemaa on liiga suur.
```java
public synchronized void handleBlockChange(Connection connection, int x, int y, int z, int blockType) {
    if (connection != null) {
        Player player = players.stream()
                .filter(p -> p.getConnection().equals(connection))
                .findFirst()
                .orElse(null);

        if (player != null) {
            float px = player.getPhysicsState().getX();
            float py = player.getPhysicsState().getY();
            float pz = player.getPhysicsState().getZ();

            double distance = Math.sqrt(Math.pow(x - px, 2) + Math.pow(y - py, 2) + Math.pow(z - pz, 2));
            if (distance > 6.0) {
                return; // Mängija on liiga kaugel, ignoreeri ehitust!
            }
        }
    }
    // ... originaalne kood jätkub ...
```
</details>

### 5. Serveri ülekoormamine chunkidega (DoS rünnaku ennetus)
Pahatahtlik klient võib saata tuhandeid `ChunkRequestMessage` päringuid koodinaatidele, mis asuvad maailma lõpus, koormates serveri üle. Server peab kontrollima, et mängija küsib *chunke* ainult enda lähedalt (nt mitte kaugemalt kui 3 chunki raadiuses).

<details> 
<summary>💡 Vihje 1</summary> 

Vaata meetodit `GameInstance.handleChunkRequest`. Leia uuesti mängija, arvuta mängija koordinaatidest tema hetkene chunk (`px / Chunk.SIZE_X`) ja võrdle seda küsitud chunkiga.
</details>

<details> 
<summary>🛠 Lahendus</summary> 

```java
public synchronized void handleChunkRequest(Connection connection, int chunkX, int chunkZ) {
    Player player = players.stream()
            .filter(p -> p.getConnection().equals(connection))
            .findFirst()
            .orElse(null);

    if (player != null) {
        int pCx = (int) Math.floor(player.getPhysicsState().getX() / Chunk.SIZE_X);
        int pCz = (int) Math.floor(player.getPhysicsState().getZ() / Chunk.SIZE_Z);

        if (Math.abs(pCx - chunkX) > 3 || Math.abs(pCz - chunkZ) > 3) {
            return; // Ignoreeri ebaseaduslikku päringut
        }
    }
    // ... algne chunk'i genereerimise kood ...
```
</details>

### 6. Puulehtede läbipaistvaks muutmine tehisintellekti abil (Shaders & Meshing)
Hetkel on puulehed (Leaves) paksud ja läbipaistmatud rohelised plokid. Kuna graafika renderdatakse protseduuriliselt koodis, on sinu ülesanne muuta puulehed (`BlockConstants.MAT_LEAVES`) läbipaistvaks (auguliseks). **Soovitame anda see ülesanne lahendamiseks oma AI agendile** (nt Copilot või Antigravity).

Sööda AI-le järgnev *prompt*:
*"Selles Java + LibGDX vokselmängus genereeritakse 3D meshid failis `VoxelMeshBuilder.java` ja värvitakse GLSL-is failis `Shaders.java`. Puulehtede ID on 5 (`BlockConstants.MAT_LEAVES`). Ma soovin muuta puulehed varjutajas auguliseks (alpha cutout / discard) kasutades olemasolevat noise funktsiooni. Mida ma pean nendes kahes failis muutma?"*

<details> 
<summary>💡 Vihje 1 (AI pime nurk)</summary> 

Tõenäoliselt annab AI sulle ideaalse GLSL koodi, kus kasutatakse märksõna `discard;` (mis jätab piksli joonistamata). Kui sa selle koodi lisad, avastad, et lehtedesse tekivad augud, **aga aukude taga on tühjus, mitte teised plokid!**

**Miks?** Sest vokselmootor peidab jõudluse säästmiseks ära kõik sisemised tahud (*face culling*). Mootor arvab endiselt, et puuleht on täielikult läbipaistmatu.
</details>

<details> 
<summary>💡 Vihje 2</summary> 

Tahkude joonistamise otsus tehakse klassis `VoxelMeshBuilder.java` meetodis `shouldDrawFace`. Pead süsteemile ütlema, et kui naaberplokk (`nMat`) on puuleht, siis ei tohi praegust tahku peita, sest me näeme puulehest läbi!
</details>

<details> 
<summary>🛠 Lahendus</summary> 

**1. Java koodi muudatus (`VoxelMeshBuilder.java`):**
Leia meetod `shouldDrawFace` ja uuenda viimast tagastust:
```java
// Transparent vs opaque => visible
return nMat == BlockConstants.MAT_WATER || nMat == BlockConstants.MAT_GLASS || nMat == BlockConstants.MAT_LEAVES;
```

**2. GLSL Shaderi muudatus (`Shaders.java` -> FRAG koodiplokk):**
```glsl
else if (id == 5) { // LEAVES
    float n = noiseSmall(pos.xz * 1.2 + pos.y * 2.0); // lisame Y telje
    if (n > 0.55) discard; // Tekitame augu!
    return clamp(u_mat_leaves + vec3(n * 0.12), 0.0, 1.0);
}
```
</details>

### 7. BOONUS: Bioomid ja reljeefi parandamine AI abil
Kui vaatad maailma maastikku, märkad, et see on ebaloomulikult regulaarne – justkui lõputu "munarest". Põhjus on selles, et failis `TerrainGenerator.java` arvutatakse maastiku kõrgus (`calculateSurfaceHeight`) primitiivsete `Math.sin` ja `Math.cos` funktsioonide abil.

**Sinu ülesanne:** Palu oma AI agendil kirjutada parem maastikugeneratsioon, mis kasutaks näiteks *Perlin noise*'i, et luua erinevaid bioome (kõrged mäed ja lamedad tasandikud).

<details>
<summary>💡 Vihje</summary>
Keerulisema mitme bioomiga süsteemi loomine on täiesti vabatahtlik, aga isegi lihtne müra-põhine kõrguskaardi parandamine muudaks maailma palju huvitavamaks!

Näiteks kirjuta AI agendile lühidalt:

Meetodi `calculateSurfaceHeight` genereeritud maastik on igav ja korduv. Tee see ilusamaks ja efektiivsemaks, et tekiksid mäed ja orud.
</details>

<br><br>
