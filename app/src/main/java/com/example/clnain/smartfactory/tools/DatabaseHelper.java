package com.example.clnain.smartfactory.tools;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// MainActivity 和 ChartActivity 的导入是为了 insert 和 search 方法的参数类型，
// 但方法体内部不一定直接使用这些Activity实例的特定功能。
import com.example.clnain.ChartActivity;
import com.example.clnain.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper"; // 日志TAG
    private static final String DB_NAME = "smartfactory"; // 数据库名称
    private static final int DB_VERSION = 1; // 数据库版本

    // 表名和列名常量，便于维护和避免硬编码字符串
    public static final String TABLE_DATA = "data";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_HUMIDITY = "humidity";
    public static final String COLUMN_LIGHT = "light";

    // 保存传入的Context，最好是ApplicationContext，以避免Activity泄漏
    private Context mContext;

    /**
     * DatabaseHelper构造函数。
     * @param context Context实例，通常是调用者的Activity或Application。
     */
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.mContext = context.getApplicationContext(); // 保存ApplicationContext
        Log.d(TAG, "DatabaseHelper已初始化。数据库名: " + DB_NAME + ", 版本: " + DB_VERSION);
    }

    /**
     * 当数据库首次创建时调用。
     * @param db SQLiteDatabase实例。
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate: 正在创建数据库表...");
        // 构建创建表的SQL语句
        String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_DATA + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + // id作为主键并自增
                COLUMN_TEMPERATURE + " TEXT, " +                   // 温度值，以文本形式存储
                COLUMN_HUMIDITY + " TEXT, " +                      // 湿度值
                COLUMN_LIGHT + " TEXT)";                           // 光照值
        db.execSQL(CREATE_TABLE_SQL); // 执行SQL语句
        Log.d(TAG, "数据表 " + TABLE_DATA + " 已创建。");

        // 创建触发器，限制表中数据条数，保留最新的20条
        // 当插入新数据后，如果总数超过20，则删除最早的 (COUNT - 20) 条记录
        db.beginTransaction(); // 开始事务
        try {
            String CREATE_TRIGGER_SQL = "CREATE TRIGGER trigger_delete_oldest_data " +
                    "AFTER INSERT ON " + TABLE_DATA + " " + // 在向data表插入数据之后触发
                    "WHEN (SELECT COUNT(" + COLUMN_ID + ") FROM " + TABLE_DATA + ") > 20 " + // 条件：当表中记录数大于20时
                    "BEGIN " +
                    "DELETE FROM " + TABLE_DATA + " WHERE " + COLUMN_ID + " IN " +
                    "(SELECT " + COLUMN_ID + " FROM " + TABLE_DATA + " ORDER BY " + COLUMN_ID + " ASC LIMIT (SELECT COUNT(" + COLUMN_ID + ") - 20 FROM " + TABLE_DATA + "));" + // 删除ID最小（即最早插入）的多余记录
                    "END;";
            db.execSQL(CREATE_TRIGGER_SQL);
            db.setTransactionSuccessful(); // 标记事务成功
            Log.d(TAG, "触发器 trigger_delete_oldest_data 已成功创建。");
        } catch (Exception e) {
            Log.e(TAG, "创建触发器 trigger_delete_oldest_data 时发生错误", e);
        } finally {
            db.endTransaction(); // 结束事务（如果成功则提交，否则回滚）
        }
    }

    /**
     * 实际执行数据插入操作的私有/包级可见方法。
     * @param temp 温度值
     * @param hum 湿度值
     * @param light 光照值
     */
    public void insertData(String temp, String hum, String light) {
        // 检查输入数据是否为null
        if (temp == null || hum == null || light == null) {
            Log.w(TAG, "insertData: 尝试插入null数据，操作已取消。温度=" + temp + ", 湿度=" + hum + ", 光照=" + light);
            return;
        }
        SQLiteDatabase db = null; // 声明SQLiteDatabase变量
        try {
            db = this.getWritableDatabase(); // 获取可写数据库实例
            ContentValues cv = new ContentValues(); // 创建ContentValues对象，用于存放要插入的数据
            cv.put(COLUMN_TEMPERATURE, temp);
            cv.put(COLUMN_HUMIDITY, hum);
            cv.put(COLUMN_LIGHT, light);
            long result = db.insert(TABLE_DATA, null, cv); // 执行插入操作
            if (result == -1) { // 如果返回-1，表示插入失败
                Log.e(TAG, "插入数据失败: 温度=" + temp + ", 湿度=" + hum + ", 光照=" + light);
            } else { // 否则，result是新插入行的row ID
                Log.d(TAG, "数据插入成功: ID=" + result + ", 温度=" + temp + ", 湿度=" + hum + ", 光照=" + light);
            }
        } catch (Exception e) { // 捕获所有可能的异常
            Log.e(TAG, "插入数据时发生错误: 温度=" + temp + ", 湿度=" + hum + ", 光照=" + light, e);
        } finally {
            // 通常SQLiteOpenHelper会管理数据库连接的关闭。
            // 如果在这里获取了db实例，在某些设计模式下可能需要手动关闭，但通常不建议在每次操作后都关闭。
            // if (db != null && db.isOpen()) {
            //     db.close();
            // }
        }
    }

    /**
     * 公开的插入数据方法，供MainActivity调用。
     * @param context 调用者Context (当前实现中未使用，但保留以兼容旧调用或未来扩展)
     * @param tempValue 温度值
     * @param humValue 湿度值
     * @param lightValue 光照值
     */
    public void insert(Context context, String tempValue, String humValue, String lightValue) {
        // 此处的context参数当前实现未使用，因为insertData内部通过this.getWritableDatabase()获取数据库。
        // DatabaseHelper在构造时已获得Context。
        Log.d(TAG, "insert方法被调用，将转发到insertData。温度=" + tempValue + ", 湿度=" + humValue + ", 光照=" + lightValue);
        insertData(tempValue, humValue, lightValue); // 调用实际的插入方法
    }


    /**
     * 根据数据类型从数据库查询最新的20条历史数据。
     * @param context Context (当前实现中未使用，因为数据库操作基于helper自身context)
     * @param type 要查询的数据类型 ("温度", "湿度", "光照")
     * @return 包含浮点数值的列表；如果类型未知或无数据，则返回空列表。
     */
    @SuppressLint("Range") // 抑制Range相关的lint警告，因为我们明确知道列名
    public List<Float> search(Context context, String type) {
        List<Float> data = new ArrayList<>(); // 存储查询结果的列表
        SQLiteDatabase db = null; // SQLiteDatabase实例
        Cursor c = null;          // Cursor实例，用于遍历查询结果
        String columnToQuery;     // 要查询的列名

        // 根据传入的类型确定要查询的数据库列
        switch (type) {
            case "温度":
                columnToQuery = COLUMN_TEMPERATURE;
                break;
            case "湿度":
                columnToQuery = COLUMN_HUMIDITY;
                break;
            case "光照":
                columnToQuery = COLUMN_LIGHT;
                break;
            default:
                Log.w(TAG, "未知的查询数据类型: " + type);
                return data; // 对于未知类型，返回空列表
        }
        Log.d(TAG, "正在查询历史数据: 类型=" + type + ", 列名=" + columnToQuery);

        try {
            db = this.getReadableDatabase(); // 获取可读数据库实例
            // 构建SQL查询语句:
            // 1. 内层查询: 从data表中按id降序选择最新的20条记录的指定列和id。
            // 2. 外层查询: 将这20条记录按id升序排列，这样数据点适合图表从左到右（旧到新）显示。
            String query = "SELECT " + columnToQuery + " FROM " +
                    "(SELECT " + columnToQuery + ", " + COLUMN_ID + " FROM " + TABLE_DATA + " ORDER BY " + COLUMN_ID + " DESC LIMIT 20) " +
                    "ORDER BY " + COLUMN_ID + " ASC";
            c = db.rawQuery(query, null); // 执行查询

            // 遍历Cursor结果集
            if (c != null && c.moveToFirst()) { // 确保Cursor不为null且至少有一条记录
                do {
                    // 根据列名获取字符串值 (因为数据库中存的是TEXT)
                    String valueStr = c.getString(c.getColumnIndex(columnToQuery));
                    if (valueStr != null && !valueStr.isEmpty()) { // 确保值不为null或空
                        try {
                            data.add(Float.parseFloat(valueStr)); // 转换为Float并添加到列表
                        } catch (NumberFormatException e) {
                            // 如果字符串不能转换为Float (例如存了非数字)，记录错误
                            Log.e(TAG, "解析Float时发生错误: '" + valueStr + "' (类型: " + type + ")", e);
                            // data.add(0f); // 可选：发生解析错误时添加一个默认值，如0
                        }
                    } else {
                        // 如果数据库中的值为null或空字符串
                        Log.w(TAG, "在记录中发现类型 " + type + " 的值为null或空。");
                        // data.add(0f); // 可选：为null或空值添加一个默认值
                    }
                } while (c.moveToNext()); //移动到下一条记录
            } else {
                Log.d(TAG, "未找到类型为 " + type + " 的数据。");
            }
        } catch (Exception e) { // 捕获所有可能的查询异常
            Log.e(TAG, "查询类型为 " + type + " 的数据时发生错误", e);
        } finally {
            // 关闭Cursor和数据库连接（如果适用）
            if (c != null) {
                c.close(); // 务必关闭Cursor
            }
            // SQLiteOpenHelper通常会管理db的关闭，此处一般不需要手动db.close()
        }
        Log.d(TAG, "类型为 '" + type + "' 的查询完成，共找到 " + data.size() + " 条记录。");
        return data; // 返回数据列表
    }

    /**
     * ChartActivity调用的公共搜索方法。
     * @param chartActivity ChartActivity的实例 (主要用于获取Context)
     * @param dataType 要查询的数据类型
     * @return 包含历史数据的列表
     */
    public List<Float> search(ChartActivity chartActivity, String dataType) {
        Log.d(TAG, "ChartActivity调用search方法，查询类型: " + dataType);
        // ChartActivity本身就是一个Context，可以直接传递给通用的search方法。
        // 或者使用DatabaseHelper构造时传入的mContext。
        return search((Context)chartActivity, dataType); // 调用通用的search方法
    }

    /**
     * 当数据库需要升级时调用 (例如DB_VERSION增加时)。
     * @param db SQLiteDatabase实例
     * @param oldVersion 旧的数据库版本号
     * @param newVersion 新的数据库版本号
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "正在从版本 " + oldVersion + " 升级到版本 " + newVersion + "，旧数据将被销毁。");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA); // 删除旧表
        onCreate(db); // 重新创建表结构
    }

    // MainActivity实际调用的是 insert(Context context, String tempValue, String humValue, String lightValue)
    // 而该方法已在上面正确实现为调用 insertData。
    // 因此，之前可能存在的 public void insert(MainActivity mainActivity, ...) 方法签名已统一。
}