package cn.cookiestudio.gun;

import cn.cookiestudio.gun.command.GunCommand;
import cn.cookiestudio.gun.guns.GunData;
import cn.cookiestudio.gun.guns.ItemGunBase;
import cn.cookiestudio.gun.guns.achieve.*;
import cn.cookiestudio.gun.playersetting.PlayerSettingPool;
import cn.nukkit.Server;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Getter
public class GunPlugin extends PluginBase {

    @Getter
    private static GunPlugin instance;
    private Config config;
    private Map<Class<? extends ItemGunBase>, GunData> gunDataMap = new HashMap<>();
    private Map<String, Class<? extends ItemGunBase>> stringClassMap = new HashMap<>();
    private CoolDownTimer coolDownTimer;
    private Skin crateSkin;
    private Skin ammoBoxSkin;
    private PlayerSettingPool playerSettingPool;
    private FireTask fireTask;

    {
        stringClassMap.put("akm", ItemGunAkm.class);
        stringClassMap.put("awp", ItemGunAwp.class);
        stringClassMap.put("barrett", ItemGunBarrett.class);
        stringClassMap.put("m3", ItemGunM3.class);
        stringClassMap.put("m249", ItemGunM249.class);
        stringClassMap.put("mk18", ItemGunMk18.class);
        stringClassMap.put("mp5", ItemGunMp5.class);
        stringClassMap.put("p90", ItemGunP90.class);
        stringClassMap.put("taurus", ItemGunTaurus.class);
    }

    @Override
    public void onEnable() {
        instance = this;
        playerSettingPool = new PlayerSettingPool();
        fireTask = new FireTask(this);
        initCrateSkin();
        copyResource();
        config = new Config(getDataFolder() + "/config.yml");
        loadGunData();
        registerListener();
        registerCommand();
        coolDownTimer = new CoolDownTimer();
    }

    private void copyResource(){
        saveDefaultConfig();
        Path p = Paths.get(Server.getInstance().getDataPath() + "resource_packs/gun.zip");
        if (!Files.exists(p)){
            this.getLogger().warning("未在目录" + p.toString() + "下找到材质包，正在复制，请在完成后重启服务器应用更改");
            try {
                Files.copy(this.getClass().getClassLoader().getResourceAsStream("resources/gun.zip"),p);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void loadGunData() {
        Map<String, Object> map = config.getAll();
        map.entrySet().stream().forEach(e -> {
            Map<String, Object> value = (Map<String, Object>) e.getValue();
            GunData gunData = GunData
                    .builder()
                    .gunName(e.getKey())
                    .magName((String) value.get("magName"))
                    .hitDamage((Double) value.get("hitDamage"))
                    .fireCoolDown((Double) value.get("fireCoolDown"))
                    .magSize((Integer) value.get("magSize"))
                    .slownessLevel((int) value.get("slownessLevel"))
                    .slownessLevelAim((int) value.get("slownessLevelAim"))
                    .particle((String) value.get("particle"))
                    .reloadTime((Double) value.get("reloadTime"))
                    .range((Double) value.get("range"))
                    .recoil((Double) value.get("recoil"))
                    .fireSwingIntensity((Double) value.get("fireSwingIntensity"))
                    .fireSwingDuration((Double) value.get("fireSwingDuration"))
                    .build();
            gunDataMap.put(stringClassMap.get(e.getKey()), gunData);
            try {
                ItemGunBase itemGun = stringClassMap.get(e.getKey()).newInstance();
                Item.registerCustomItem(itemGun.getClass());
                Item.registerCustomItem(itemGun.getItemMagObject().getClass());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void initCrateSkin(){
        crateSkin = new Skin();
        try {
            crateSkin.setTrusted(true);
            crateSkin.setGeometryData(new String(getBytes(GunPlugin.getInstance().getResource("resources/model/crate/skin.json"))));
            crateSkin.setGeometryName("geometry.crate");
            crateSkin.setSkinId("crate");
            crateSkin.setSkinData(ImageIO.read(GunPlugin.getInstance().getResource("resources/model/crate/skin.png")));
        } catch (Throwable e) {
            e.printStackTrace();
        }

        ammoBoxSkin = new Skin();
        try {
            ammoBoxSkin.setTrusted(true);
            ammoBoxSkin.setGeometryData(new String(getBytes(GunPlugin.getInstance().getResource("resources/model/ammobox/skin.json"))));
            ammoBoxSkin.setGeometryName("geometry.ammobox");
            ammoBoxSkin.setSkinId("ammobox");
            ammoBoxSkin.setSkinData(ImageIO.read(GunPlugin.getInstance().getResource("resources/model/ammobox/skin.png")));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static byte[] getBytes(InputStream inStream) throws Exception{
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while((len=inStream.read(buffer))!=-1){
            outStream.write(buffer,0,len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }


    private void registerListener() {
        Server.getInstance().getPluginManager().registerEvents(new Listener(), this);
    }

    private void registerCommand() {
        Server.getInstance().getCommandMap().register("",new GunCommand("gun"));
    }

    public void saveGunData(GunData gunData){
        String gunName = gunData.getGunName();
        config.set(gunName + ".magSize",gunData.getMagSize());
        config.set(gunName + ".fireCoolDown",gunData.getFireCoolDown());
        config.set(gunName + ".reloadTime",gunData.getReloadTime());
        config.set(gunName + ".slownessLevel",gunData.getSlownessLevel());
        config.set(gunName + ".slownessLevelAim",gunData.getSlownessLevelAim());
        config.set(gunName + ".fireSwingIntensity",gunData.getFireSwingIntensity());
        config.set(gunName + ".fireSwingDuration",gunData.getFireSwingDuration());
        config.set(gunName + ".hitDamage",gunData.getHitDamage());
        config.set(gunName + ".range",gunData.getRange());
        config.set(gunName + ".particle",gunData.getParticle());
        config.set(gunName + ".magName",gunData.getMagName());
        config.set(gunName + ".recoil",gunData.getRecoil());
        config.save();
    }
}
