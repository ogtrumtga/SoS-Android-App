$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$diagramSource = @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Drawing.Text;
using System.IO;

public sealed class FlowCanvas : IDisposable {
    public readonly Bitmap Image;
    public readonly Graphics G;
    public readonly float Width;
    public readonly float Height;
    public static readonly Color Ink = ColorTranslator.FromHtml("#172B42");
    public static readonly Color Muted = ColorTranslator.FromHtml("#52667C");
    public static readonly Color Http = ColorTranslator.FromHtml("#1665B5");
    public static readonly Color Ws = ColorTranslator.FromHtml("#7941B0");
    public static readonly Color Gps = ColorTranslator.FromHtml("#16764D");
    public static readonly Color External = ColorTranslator.FromHtml("#A45B13");
    public int OverflowCount = 0;

    public FlowCanvas(float width, float height) {
        Width = width; Height = height;
        Image = new Bitmap((int)(width * 2), (int)(height * 2));
        Image.SetResolution(192, 192);
        G = Graphics.FromImage(Image);
        G.ScaleTransform(2, 2);
        G.SmoothingMode = SmoothingMode.AntiAlias;
        G.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
        G.Clear(Color.White);
    }
    public void Dispose() { G.Dispose(); Image.Dispose(); }
    public Font Font(float size, bool bold = false) {
        return new Font("Segoe UI", size, bold ? FontStyle.Bold : FontStyle.Regular, GraphicsUnit.Pixel);
    }
    public void Text(string s, float x, float y, float w, float h, float size = 19,
                     Color? color = null, bool bold = false, bool centered = false) {
        using (var font = Font(size, bold))
        using (var brush = new SolidBrush(color ?? Ink))
        using (var format = new StringFormat()) {
            format.Alignment = centered ? StringAlignment.Center : StringAlignment.Near;
            format.LineAlignment = StringAlignment.Center;
            format.Trimming = StringTrimming.None;
            SizeF measured = G.MeasureString(s, font, new SizeF(w, 10000), format);
            if (measured.Height > h + 2) {
                OverflowCount++;
                Console.WriteLine("TEXT OVERFLOW: " + s.Replace("\n", " | ") + " [" + measured.Height + " > " + h + "]");
            }
            G.DrawString(s, font, brush, new RectangleF(x, y, w, h), format);
        }
    }
    private GraphicsPath Rounded(float x, float y, float w, float h, float r) {
        var p = new GraphicsPath(); float d = 2 * r;
        p.AddArc(x, y, d, d, 180, 90);
        p.AddArc(x+w-d, y, d, d, 270, 90);
        p.AddArc(x+w-d, y+h-d, d, d, 0, 90);
        p.AddArc(x, y+h-d, d, d, 90, 90);
        p.CloseFigure(); return p;
    }
    public void Box(float x, float y, float w, float h, string title, string body,
                    Color? accent = null, float bodySize = 20) {
        Color a = accent ?? Ink;
        Color fill = Color.FromArgb(255, (int)(a.R*.055+255*.945), (int)(a.G*.055+255*.945), (int)(a.B*.055+255*.945));
        using (var path = Rounded(x,y,w,h,12))
        using (var brush = new SolidBrush(fill))
        using (var pen = new Pen(Color.FromArgb(80,a), 1.2f)) {
            G.FillPath(brush,path); G.DrawPath(pen,path);
        }
        Text(title,x+20,y+12,w-40,30,19,a,true);
        if (!String.IsNullOrEmpty(body)) Text(body,x+20,y+46,w-40,h-58,bodySize,Ink);
    }
    public void Arrow(PointF[] points, Color? color = null, bool dashed = false, float thickness = 2) {
        Color c = color ?? Muted;
        using (var pen = new Pen(c,thickness)) {
            if(dashed) pen.DashPattern = new float[]{4,3};
            G.DrawLines(pen,points);
        }
        PointF a=points[points.Length-2], b=points[points.Length-1];
        double angle=Math.Atan2(b.Y-a.Y,b.X-a.X);
        float len=10, half=4.8f;
        PointF p1=new PointF(b.X-(float)Math.Cos(angle)*len+(float)Math.Sin(angle)*half,
                            b.Y-(float)Math.Sin(angle)*len-(float)Math.Cos(angle)*half);
        PointF p2=new PointF(b.X-(float)Math.Cos(angle)*len-(float)Math.Sin(angle)*half,
                            b.Y-(float)Math.Sin(angle)*len+(float)Math.Cos(angle)*half);
        using(var brush=new SolidBrush(c)) G.FillPolygon(brush,new PointF[]{b,p1,p2});
    }
    public void Arrow(float x1,float y1,float x2,float y2,Color? color=null,bool dashed=false) {
        Arrow(new PointF[]{new PointF(x1,y1),new PointF(x2,y2)},color,dashed);
    }
    public void Legend(float y) {
        string[] labels={"HTTP API: backend SOS","WS: WebSocket","GPS: thiết bị","HTTP ngoài: nền / OSRM"};
        Color[] colors={Http,Ws,Gps,External};
        float[] xs={45,365,635,855};
        for(int i=0;i<labels.Length;i++) {
            using(var brush=new SolidBrush(colors[i])) G.FillEllipse(brush,xs[i],y+11,10,10);
            Text(labels[i],xs[i]+18,y,320,33,17,Muted);
        }
    }
    public void Header(string title,string subtitle) {
        Text(title,45,30,1210,53,35,Ink,true);
        Text(subtitle,45,88,1210,40,19,Muted);
        Legend(140);
    }
    public void Save(string path) {
        Directory.CreateDirectory(Path.GetDirectoryName(path));
        Image.Save(path,ImageFormat.Png);
        Console.WriteLine(path + " | " + Image.Width + " x " + Image.Height + " | text overflow: " + OverflowCount);
        if(OverflowCount!=0) throw new Exception("Text does not fit.");
    }
}

public static class ExportFlowDiagrams {
    public static void Map(string path) {
        using(var c=new FlowCanvas(1300,1640)) {
            c.Header("01  LUỒNG HIỂN THỊ MAP", "HomeActivity • Nền bản đồ, dữ liệu SOS và GPS được chuẩn bị độc lập.");
            c.Box(350,215,600,100,"ANDROID MỞ MÀN HÌNH","Android gọi HomeActivity.onCreate().");
            c.Arrow(650,315,650,345);
            c.Box(320,345,660,120,"1  ĐĂNG KÝ VÀ DỰNG GIAO DIỆN","bind(\"public\", ...) đăng ký callback.\nsetupMapScreen() + initMap() tạo bản đồ.");

            c.Arrow(new PointF[]{new PointF(400,465),new PointF(400,495),new PointF(320,495),new PointF(320,525)});
            c.Arrow(new PointF[]{new PointF(900,465),new PointF(900,495),new PointF(920,495),new PointF(920,525)});
            c.Box(65,525,510,130,"2A  KHỞI TẠO NỀN BẢN ĐỒ","mapView.getMapAsync()\n→ MapHelper.applyDefaultStyle()",FlowCanvas.External);
            c.Box(665,525,510,130,"2B  ANDROID GỌI onStart()","refresh() • realtime.connect() • gps.request()\nKhông chờ tải nền bản đồ xong.",null,19);

            c.Arrow(320,655,320,705,FlowCanvas.External);
            c.Box(65,705,510,150,"3A  CALLBACK STYLE","mapReady = true → renderMap().\nSDK tự tải style ngoài nếu cần và tile\nqua HTTP; tile có thể còn đang tải.",FlowCanvas.External);
            c.Arrow(920,655,920,705,FlowCanvas.Http);
            c.Box(665,705,510,150,"3B  HTTP API: LẤY ĐIỂM SOS","execute(\"public\", sos::loadPublicMap)\n→ SosViewModel → BaseApiViewModel\n→ ApiClient → GET /api/sos/public/map",FlowCanvas.Http,18);

            c.Arrow(920,855,920,900,FlowCanvas.Http);
            c.Box(665,900,510,145,"4  KẾT QUẢ QUAY VỀ UI","JSON → PublicMapResponse\n→ LiveData SUCCESS → bind(\"public\")\n→ lưu points → renderMap()",FlowCanvas.Http,19);
            c.Box(65,900,510,145,"GPS THIẾT BỊ","onStart() → gps.request()\n→ DeviceLocation nhận tọa độ mới\n→ lưu latest → gọi renderMap()",FlowCanvas.Gps,19);

            c.Arrow(new PointF[]{new PointF(65,780),new PointF(35,780),new PointF(35,1115),new PointF(320,1115),new PointF(320,1160)},FlowCanvas.External);
            c.Arrow(new PointF[]{new PointF(320,1045),new PointF(320,1090),new PointF(510,1090),new PointF(510,1160)},FlowCanvas.Gps);
            c.Arrow(new PointF[]{new PointF(920,1045),new PointF(920,1090),new PointF(710,1090),new PointF(710,1160)},FlowCanvas.Http);
            c.Box(215,1160,690,120,"5  VẼ LẠI TỪ TRẠNG THÁI HIỆN CÓ","renderMap(): chỉ chạy khi mapReady.\nSosMapData.drawPublic(map, points, GPS, bộ lọc, ...)");
            c.Arrow(560,1280,560,1325);
            c.Box(215,1325,690,115,"6  HIỂN THỊ LÊN MapView","map.clear() → thêm marker BẠN và SOS\n→ MapHelper căn camera khi cần.");

            c.Box(965,1160,285,280,"WS CONNECT / NHẬN","onStart()\n→ realtime.connect()\n→ newWebSocket\n/ws?token=...\n\nNhận sự kiện liên quan\n→ refresh() → gọi lại\nHTTP API ở bước 3B.",FlowCanvas.Ws,18);
            c.Arrow(new PointF[]{new PointF(1175,590),new PointF(1270,590),new PointF(1270,1120),new PointF(1107,1120),new PointF(1107,1160)},FlowCanvas.Ws,true);
            c.Arrow(new PointF[]{new PointF(1250,1310),new PointF(1285,1310),new PointF(1285,780),new PointF(1175,780)},FlowCanvas.Ws,true);

            c.Text("refresh() còn gọi GET /api/emergency-types và GET /api/sos/my-active cho bộ lọc và ca đang hoạt động.",45,1480,1210,52,18,FlowCanvas.Muted);
            c.Text("HomeActivity không subscribeSos(id). WS báo thay đổi; danh sách điểm SOS được tải lại bằng HTTP API.",45,1540,1210,52,18,FlowCanvas.Ws,true);
            c.Save(path);
        }
    }

    private static float[] Lanes={190,530,885,1170};
    private static void Phase(FlowCanvas c,float y,string title) {
        using(var b=new SolidBrush(ColorTranslator.FromHtml("#EDF2F7"))) c.G.FillRectangle(b,45,y,1210,43);
        c.Text(title,60,y,1180,43,19,FlowCanvas.Ink,true);
    }
    private static void Message(FlowCanvas c,int from,int to,float y,string label,Color? color=null,bool reply=false,float size=18) {
        float x1=Lanes[from],x2=Lanes[to];
        float left=Math.Min(x1,x2)+9;
        float width=Math.Abs(x2-x1)-18;
        float labelHeight=label.Split('\n').Length>2 ? 78 : 54;
        using(var b=new SolidBrush(Color.White)) c.G.FillRectangle(b,left,y-labelHeight-4,width,labelHeight);
        c.Text(label,left,y-labelHeight-4,width,labelHeight,size,color??FlowCanvas.Ink,false,true);
        c.Arrow(x1,y,x2,y,color,reply);
    }
    private static void Note(FlowCanvas c,float y,string s,Color? accent=null,float h=66) {
        Color col=accent??FlowCanvas.Muted;
        using(var b=new SolidBrush(ColorTranslator.FromHtml("#F7F9FC"))) c.G.FillRectangle(b,60,y,1180,h);
        c.Text(s,76,y+6,1148,h-12,18,col,false,true);
    }
    public static void Sos(string path) {
        using(var c=new FlowCanvas(1300,3180)) {
            c.Header("02  LUỒNG GỬI VÀ THEO DÕI SOS", "SendSosActivity → TrackSosActivity / LiveSosActivity • Đọc từ trên xuống.");
            string[] titles={"UI gửi / theo dõi SOS","Các ViewModel","Backend SOS","OSRM"};
            float[] widths={290,260,250,160};
            for(int i=0;i<4;i++) {
                float x=Lanes[i]-widths[i]/2;
                c.Box(x,205,widths[i],65,titles[i],"",i==3?FlowCanvas.External:FlowCanvas.Ink);
                using(var p=new Pen(ColorTranslator.FromHtml("#CED7E1"),1)) {
                    p.DashPattern=new float[]{5,4};
                    c.G.DrawLine(p,Lanes[i],270,Lanes[i],3080);
                }
            }
            Phase(c,295,"1  GỬI YÊU CẦU SOS");
            Note(c,350,"Nhập mô tả, loại sự cố, PUBLIC/PRIVATE và vị trí GPS hoặc điểm chạm. Bấm gửi → sendSos() kiểm tra dữ liệu.");
            Message(c,0,1,490,"execute(\"create\")\nSosViewModel.createSos()",FlowCanvas.Http);
            Message(c,1,2,570,"HTTP API\nPOST /api/sos",FlowCanvas.Http);
            Message(c,2,1,650,"JSON: SOS và ID",FlowCanvas.Http,true);
            Message(c,1,0,730,"LiveData SUCCESS\n→ bind(\"create\")",FlowCanvas.Http,true);
            Note(c,755,"UI mở TrackSosActivity, truyền SOS_ID → onCreate() đăng ký bind/observer bằng initLiveSos() và chuẩn bị map.");

            Phase(c,845,"2  VÀO MÀN THEO DÕI: MỞ WS VÀ TẢI DỮ LIỆU");
            Message(c,0,1,960,"Android gọi onStart()\nconnect() + refreshSos()");
            Message(c,1,2,1040,"WS CONNECT: newWebSocket\n/ws?token=...",FlowCanvas.Ws);
            Message(c,1,2,1120,"HTTP API: loadDetail()\nGET /api/sos/{id}",FlowCanvas.Http);
            Message(c,2,1,1200,"JSON: chi tiết SOS",FlowCanvas.Http,true);
            Message(c,1,0,1280,"bind(\"detail\")\n→ lưu currentSos",FlowCanvas.Http,true);
            Message(c,0,1,1360,"subscribeSos(id)\n+ execute locations / trail");
            Message(c,1,2,1440,"WS GỬI: SUBSCRIBE_SOS\n+ sos_id",FlowCanvas.Ws);
            Message(c,1,2,1540,"HTTP API: GET\n/api/location/{id}/current\n/api/location/{id}/trail",FlowCanvas.Http, false,17);
            Message(c,2,1,1625,"JSON: vị trí và lịch sử",FlowCanvas.Http,true);
            Message(c,1,0,1705,"bind cập nhật locations / trail\n→ onSosChanged() + renderLiveMap()",FlowCanvas.Http,true,17);
            Note(c,1730,"connect() và tải chi tiết chạy độc lập. SUBSCRIBE_SOS được giữ lại nếu WS chưa mở xong.\nTrail được tải với actor=RESCUER, limit=100.",FlowCanvas.Muted,74);

            Phase(c,1830,"3  GPS MỚI ĐƯỢC GỬI QUA HTTP; SỰ KIỆN VỊ TRÍ ĐƯỢC NHẬN QUA WS");
            Note(c,1885,"DeviceLocation nhận GPS mới → publishGps(). Chỉ gửi khi được phép; các lần gửi cách nhau ít nhất 6 giây.",FlowCanvas.Gps);
            Message(c,0,1,2020,"LocationViewModel\n.updateLocation()",FlowCanvas.Gps);
            Message(c,1,2,2100,"HTTP API\nPOST /api/location/{id}",FlowCanvas.Http);
            Message(c,2,1,2180,"WS NHẬN\nLOCATION_BROADCAST",FlowCanvas.Ws,true);
            Message(c,1,0,2260,"Observer cập nhật vị trí\ntheo actor VICTIM / RESCUER",FlowCanvas.Ws,true,17);

            Phase(c,2300,"4  CẬP NHẬT ĐƯỜNG ĐI VÀ VẼ LẠI");
            Message(c,0,3,2420,"Khi đủ hai tọa độ và cần tải tuyến: HTTP ngoài → OsrmRouter.fetchRoute()",FlowCanvas.External);
            Message(c,3,0,2500,"Các điểm tuyến đường, khoảng cách và thời gian → currentSos.setOsrmRoute()",FlowCanvas.External,true);
            Note(c,2525,"onSosChanged() cập nhật chữ/nút. renderLiveMap() → SosMapData.draw() → marker, đường và camera.\nNếu chưa có tuyến: vẽ lịch sử vị trí; nếu thiếu lịch sử thì nối thẳng hai vị trí.",null,80);

            Phase(c,2630,"5  TẢI ĐỊNH KỲ VÀ KẾT THÚC CA");
            Note(c,2685,"Mỗi 15 giây hoặc nhận sự kiện đổi trạng thái: refreshSos() → HTTP GET chi tiết → cập nhật UI.\nSau khi chi tiết tải thành công, app tiếp tục tải current và trail.",FlowCanvas.Http,80);
            Message(c,2,1,2860,"WS NHẬN: SOS_COMPLETED\nhoặc SOS_CANCELLED",FlowCanvas.Ws,true);
            Message(c,1,0,2940,"endTracking()\n→ onSosEnded()",FlowCanvas.Ws,true);
            Note(c,2965,"Dừng GPS, polling, hủy request vị trí, ngắt WS → chuyển màn kết quả.\nNếu rời màn trước khi ca kết thúc: onStop() cũng dừng cập nhật; onStart() kết nối lại.",null,80);
            c.Text("HTTP: thao tác và tải dữ liệu  •  WS: kết nối, đăng ký ca và nhận sự kiện  •  OSRM: tuyến đường bên ngoài backend SOS",45,3100,1210,50,18,FlowCanvas.Muted);
            c.Save(path);
        }
    }
}
public static class DetailedFlowDiagrams {
    private sealed class Step {
        public string Title, Body;
        public Color Color;
        public Step(string title,string body,Color color) { Title=title; Body=body; Color=color; }
    }
    private static float BodyHeight(FlowCanvas c,string body,float w,float size) {
        using(var f=c.Font(size)) using(var sf=new StringFormat()) {
            return (float)Math.Ceiling(c.G.MeasureString(body,f,new SizeF(w-40,20000),sf).Height)+72;
        }
    }
    private static RectangleF Card(FlowCanvas c,float x,float y,float w,Step s,float size=18) {
        float h=Math.Max(120,BodyHeight(c,s.Body,w,size));
        c.Box(x,y,w,h,s.Title,s.Body,s.Color,size);
        return new RectangleF(x,y,w,h);
    }
    private static float CX(RectangleF r) { return r.X+r.Width/2; }
    private static void Down(FlowCanvas c,RectangleF a,RectangleF b,Color col) {
        c.Arrow(new PointF[]{new PointF(CX(a),a.Bottom),new PointF(CX(a),b.Top-20),new PointF(CX(b),b.Top-20),new PointF(CX(b),b.Top)},col);
    }
    private static void Right(FlowCanvas c,RectangleF a,RectangleF b,Color col) {
        float y=Math.Min(a.Height,b.Height)/2+a.Top;
        c.Arrow(a.Right,y,b.Left,y,col);
    }
    private static RectangleF[] Chain(FlowCanvas c,float x,float y,float w,Step[] steps) {
        var result=new RectangleF[steps.Length];
        for(int i=0;i<steps.Length;i++) {
            result[i]=Card(c,x,y,w,steps[i]);
            if(i>0) Down(c,result[i-1],result[i],steps[i].Color);
            y=result[i].Bottom+32;
        }
        return result;
    }
    private static void Section(FlowCanvas c,float y,string title,Color col) {
        using(var b=new SolidBrush(Color.FromArgb(14,col))) c.G.FillRectangle(b,45,y,1410,45);
        c.Text(title,60,y,1380,45,21,col,true);
    }
    private static void Header(FlowCanvas c,string title,string subtitle) {
        c.Text(title,45,25,1410,58,35,FlowCanvas.Ink,true);
        c.Text(subtitle,45,88,1410,45,20,FlowCanvas.Muted);
        c.Legend(145);
        c.Text("Đường dẫn .java trong ảnh tính từ: app/src/main/java/com/javafinal/",45,190,1410,40,18,FlowCanvas.Muted);
    }
    private static void Save(FlowCanvas c,string path,float usedHeight) {
        if(c.OverflowCount!=0) throw new Exception("Text overflow: "+c.OverflowCount);
        using(var cropped=c.Image.Clone(new Rectangle(0,0,c.Image.Width,(int)Math.Ceiling(usedHeight*2)),c.Image.PixelFormat)) {
            cropped.SetResolution(192,192);
            cropped.Save(path,ImageFormat.Png);
            Console.WriteLine(path+" | "+cropped.Width+" x "+cropped.Height+" | text overflow: 0");
        }
    }
    public static void Map(string path) {
        using(var c=new FlowCanvas(1500,4500)) {
            Header(c,"01  MAP — FILE VÀ HÀM THEO TỪNG BƯỚC","Đọc nhánh A, B, C từ trên xuống. Cả nền, điểm SOS và GPS đều quay về hàm renderMap().");
            var init=Card(c,45,255,690,new Step("ANDROID GỌI onCreate()","ui/user/HomeActivity.java\nonCreate(): gọi setupMapScreen(), vm(), bind(\"public\", ...)\nvà initMap(R.id.mapView, state, this::renderMap).\nvm(), bind() thuộc ui/ApiActivity.java; callback bind\nđược viết ngay trong HomeActivity.onCreate().",FlowCanvas.Ink));
            var start=Card(c,765,255,690,new Step("ANDROID TIẾP TỤC GỌI onStart()","ui/user/HomeActivity.java\nonStart() → super.onStart() → renderBanner() → refresh()\n→ realtime.connect() → gps.request().\nsuper.onStart() đi qua ui/MapApiActivity.java\nonStart() → mapView.onStart() [SDK].",FlowCanvas.Ink));
            Right(c,init,start,FlowCanvas.Muted);
            float y=Math.Max(init.Bottom,start.Bottom)+85;
            c.Text("A  NỀN BẢN ĐỒ",45,y-50,450,38,22,FlowCanvas.External,true);
            c.Text("B  HTTP API: LẤY ĐIỂM SOS",525,y-50,450,38,22,FlowCanvas.Http,true);
            c.Text("C  WS: KẾT NỐI VÀ NHẬN",1005,y-50,450,38,22,FlowCanvas.Ws,true);
            var a=Chain(c,45,y,450,new Step[]{
                new Step("A1  KHỞI TẠO SDK VÀ LAYOUT","ui/MapApiActivity.java\nsetupMapScreen(layout) gọi:\nutil/MapHelper.java → initMapLibre()\n  → MapLibre.getInstance() [SDK]\nui/ApiActivity.java → setupScreen()\n  → setContentView(activity_home).\nXML: app/src/main/res/layout/\nactivity_home.xml có MapView.",FlowCanvas.External),
                new Step("A2  ĐỢI ĐỐI TƯỢNG MAP","ui/MapApiActivity.java\ninitMap(id, savedState, render):\nfindViewById() → mapView.onCreate()\n→ MapHelper.enableScrollViewTouch()\n→ mapView.getMapAsync() [SDK].\nCallback gán map = readyMap.",FlowCanvas.External),
                new Step("A3  NẠP STYLE / NỀN","util/MapHelper.java\napplyDefaultStyle() → applyMapStyle()\n→ map.setStyle(builder, callback) [SDK].\nRaster: getRasterStyleJson().\nHTTP ngoài: SDK tự tải style nếu cần\nvà các tile từ nguồn nền đã chọn.",FlowCanvas.External),
                new Step("A4  STYLE SẴN SÀNG","ui/MapApiActivity.java\nCallback style trong initMap():\nmapReady = true → render.run().\nrender là this::renderMap đã truyền\ntừ HomeActivity.onCreate().\n→ đi tới R1 bên dưới.",FlowCanvas.External)
            });
            var b=Chain(c,525,y,450,new Step[]{
                new Step("B1  BẮT ĐẦU TẢI","ui/user/HomeActivity.java\nrefresh():\nexecute(\"public\", sos::loadPublicMap).\nrefresh() chạy từ onStart(), hoặc\nobserver WS khi có sự kiện liên quan.",FlowCanvas.Http),
                new Step("B2  THỰC HIỆN TÁC VỤ","ui/ApiActivity.java\nexecute(key, action) → action.get()\n→ gọi SosViewModel.loadPublicMap().\nSau khi nhận LiveData: giữ bằng\nui/ScreenRequests.java → put()\nvà gắn observer bằng attach().",FlowCanvas.Http),
                new Step("B3  CHỌN ENDPOINT","viewmodel/SosViewModel.java\nloadPublicMap()\n→ request(\"GET\", \"api/sos/public/map\",\n..., PublicMapResponse.class, ...).\nrequest() là hàm kế thừa từ\nviewmodel/BaseApiViewModel.java.",FlowCanvas.Http),
                new Step("B4  GỌI HTTP API THẬT","viewmodel/BaseApiViewModel.java\nrequest(): tạo ApiState.loading()\n→ api.newCall() → api.enqueue().\nnetwork/ApiClient.java\nnewCall(): tạo request OkHttp.\nenqueue(): call.enqueue() [SDK].\nHTTP GET /api/sos/public/map.",FlowCanvas.Http),
                new Step("B5  JSON → LIVE DATA","network/ApiClient.java\nCallback.onResponse() trong enqueue():\ngson.fromJson(..., responseType)\n→ deliver() → callback.onSuccess().\nviewmodel/BaseApiViewModel.java\nResult.onSuccess() trong request():\nstate.setValue(ApiState.success(data)).",FlowCanvas.Http),
                new Step("B6  CALLBACK CẬP NHẬT UI","ui/ApiActivity.java → attach()\nObserver.onChanged():\nScreenRequests.consume() →\nbinding.success.accept(state.getData()).\nui/user/HomeActivity.java\nLambda bind(\"public\") trong onCreate():\npoints = response.getData()\n→ renderMap() [R1] + updateStatus().",FlowCanvas.Http)
            });
            var ws=Chain(c,1005,y,450,new Step[]{
                new Step("C1  WS CONNECT","viewmodel/RealtimeViewModel.java\nconnect() → open(attempt).\nopen(): api.url(\"ws\") + token\n→ api.getHttp().newWebSocket(...)\n[OkHttp SDK].\nĐích kết nối: /ws?token=...",FlowCanvas.Ws),
                new Step("C2  WS NHẬN → LIVE DATA","viewmodel/RealtimeViewModel.java\nWebSocketListener.onMessage()\n(trong open()) → dispatch().\nnetwork/RealtimeProtocol.java\ndecode(): chuẩn hóa type → event.\nQuay lại onMessage():\nmessages.setValue(message).",FlowCanvas.Ws),
                new Step("C3  SỰ KIỆN → TẢI LẠI HTTP","ui/user/HomeActivity.java\nTrong onCreate():\nrealtime.getMessages().observe(...).\nLambda bỏ qua PONG, SUBSCRIBED,\nAUTH_SUCCESS; sự kiện còn lại\n→ refresh() → quay lại B1.\nHome không gọi subscribeSos(id).",FlowCanvas.Ws)
            });
            float gy=ws[ws.Length-1].Bottom+75;
            c.Text("GPS  CŨNG BẮT ĐẦU TỪ onStart()",1005,gy-45,450,36,21,FlowCanvas.Gps,true);
            var gps=Chain(c,1005,gy,450,new Step[]{
                new Step("G1  LẤY VỊ TRÍ THIẾT BỊ","util/DeviceLocation.java\nrequest() → kiểm tra / xin quyền\n→ start() khi Activity đang STARTED.\nclient.requestLocationUpdates(...)\n[FusedLocationProviderClient SDK].",FlowCanvas.Gps),
                new Step("G2  GPS MỚI → VẼ LẠI","util/DeviceLocation.java\nLocationCallback.onLocationResult():\nlatest = location\n→ onLocation.accept(location).\nui/user/HomeActivity.java\nLambda location → renderMap()\nđược truyền trong onCreate().\n→ đi tới R1 bên dưới.",FlowCanvas.Gps)
            });
            c.Arrow(new PointF[]{new PointF(CX(init),init.Bottom),new PointF(CX(init),y-72),new PointF(CX(a[0]),y-72),new PointF(CX(a[0]),y-57)},FlowCanvas.External);
            c.Arrow(new PointF[]{new PointF(CX(start),start.Bottom),new PointF(CX(start),y-72),new PointF(CX(b[0]),y-72),new PointF(CX(b[0]),y-57)},FlowCanvas.Http);
            c.Arrow(new PointF[]{new PointF(CX(start),start.Bottom),new PointF(CX(start),y-72),new PointF(CX(ws[0]),y-72),new PointF(CX(ws[0]),y-57)},FlowCanvas.Ws);
            float bottom=Math.Max(Math.Max(a[a.Length-1].Bottom,b[b.Length-1].Bottom),gps[gps.Length-1].Bottom);
            float ry=bottom+110;
            Section(c,ry-65,"R  ĐIỂM HỘI TỤ: NỀN / DỮ LIỆU / GPS THAY ĐỔI ĐỀU GỌI renderMap()",FlowCanvas.Ink);
            var r1=Card(c,45,ry,450,new Step("R1  KIỂM TRA VÀ GỌI VẼ","ui/user/HomeActivity.java\nrenderMap(): if (!mapReady) return.\nLấy gps.getLatest(), points,\nthis::matchesFilter, !centered.\n→ SosMapData.drawPublic(...).\nmatchesFilter() kiểm tra loại sự cố.\nDữ liệu đến sớm vẫn được lưu;\ncallback style sẽ gọi render lại.",FlowCanvas.Ink));
            var r2=Card(c,525,ry,450,new Step("R2  VẼ ĐIỂM CÔNG KHAI","util/SosMapData.java\ndrawPublic(): map.clear() [SDK]\n→ marker BẠN nếu có GPS\n→ duyệt points, lọc và valid(lat,lng)\n→ lấy areaLat, areaLng đã làm mờ\n→ thêm vùng polygon và marker SOS.\nModel: model/PublicSosPoint.java\ngetAreaLat(), getAreaLng().",FlowCanvas.Ink));
            var r3=Card(c,1005,ry,450,new Step("R3  ICON, VÙNG VÀ CAMERA","util/MapHelper.java\ncreatePinIcon(): Bitmap → icon.\naddFloodZonePolygon(): vùng quanh SOS.\nmoveCamera() / fitRouteBounds():\ncăn camera khi được yêu cầu.\nMapLibreMap [SDK]: addMarker(),\naddPolygon(), animateCamera().\n→ hiển thị trong MapView của XML.",FlowCanvas.Ink));
            Right(c,r1,r2,FlowCanvas.Ink); Right(c,r2,r3,FlowCanvas.Ink);
            // Route to the numbered destination without crossing another column's cards.
            c.Arrow(new PointF[]{new PointF(a[a.Length-1].Left,a[a.Length-1].Bottom-30),new PointF(23,a[a.Length-1].Bottom-30),new PointF(23,ry+75),new PointF(r1.Left,ry+75)},FlowCanvas.External);
            c.Arrow(CX(b[b.Length-1]),b[b.Length-1].Bottom,CX(b[b.Length-1]),ry-70,FlowCanvas.Http);
            c.Arrow(new PointF[]{new PointF(CX(gps[gps.Length-1]),gps[gps.Length-1].Bottom),new PointF(CX(gps[gps.Length-1]),bottom+20),new PointF(860,bottom+20),new PointF(860,ry-70)},FlowCanvas.Gps);
            c.Arrow(CX(r1),ry-17,CX(r1),ry,FlowCanvas.Ink);
            float fy=Math.Max(r1.Bottom,Math.Max(r2.Bottom,r3.Bottom))+50;
            var extra=Card(c,45,fy,690,new Step("HAI API PHỤ TRONG refresh()","viewmodel/EmergencyTypeViewModel.java → loadAll()\nGET /api/emergency-types → Home.renderTypeChips().\nviewmodel/SosViewModel.java → loadMyActive()\nGET /api/sos/my-active → Home.renderBanner().\nCác hàm Home đều nằm trong ui/user/HomeActivity.java.",FlowCanvas.Http));
            var life=Card(c,765,fy,690,new Step("VÒNG ĐỜI VÀ ĐỔI KIỂU BẢN ĐỒ","ui/user/HomeActivity.java → onStop()\n→ viewmodel/RealtimeViewModel.java → disconnect().\nutil/DeviceLocation.java → onStop(): gỡ GPS listener.\nui/MapApiActivity.java: chuyển tiếp vòng đời cho MapView.\nutil/MapHelper.java: showLayerSwitchDialog()\n→ applyMapStyle() → callback gọi Home.renderMap().",FlowCanvas.Muted));
            float end=Math.Max(extra.Bottom,life.Bottom)+70;
            c.Text("Tên callback được ghi kèm hàm chứa nó; onMessage/onResponse là callback của SDK, không phải hàm UI tự gọi.",45,end-35,1410,45,18,FlowCanvas.Muted);
            Save(c,path,end+35);
        }
    }
    public static void Sos(string path) {
        using(var c=new FlowCanvas(1500,6000)) {
            Header(c,"02  SOS — FILE VÀ HÀM THEO TỪNG BƯỚC","Gửi yêu cầu → tải ca → đăng ký WS → nhận vị trí → cập nhật map → kết thúc.");
            Section(c,255,"A  GỬI SOS: CHIỀU REQUEST VÀ CHIỀU KẾT QUẢ",FlowCanvas.Http);
            var a1=Card(c,45,325,450,new Step("A1  NHẬP LIỆU VÀ BẤM GỬI","ui/user/SendSosActivity.java\nonCreate(): bind(\"create\"), gắn nút.\nonGps() / listener chạm map: lat,lng.\nupdateSubmit(): điều kiện bật nút.\nsendSos(): kiểm tra mô tả, tọa độ,\nloại sự cố; tạo CreateSosRequest\n(model/SosApiModels.java).\n→ execute(\"create\", () -> sos.createSos(...)).",FlowCanvas.Ink));
            var a2=Card(c,525,325,450,new Step("A2  HTTP API: TẠO SOS","ui/ApiActivity.java → execute()\n→ action.get().\nviewmodel/SosViewModel.java\n→ createSos(request).\nviewmodel/BaseApiViewModel.java\n→ request(): LOADING, newCall(), enqueue().\nnetwork/ApiClient.java\n→ newCall() + enqueue() → call.enqueue().\nHTTP POST /api/sos.",FlowCanvas.Http));
            var a3=Card(c,1005,325,450,new Step("A3  RESPONSE → CHUYỂN MÀN","network/ApiClient.java\nenqueue(): Callback.onResponse()\n→ Gson → deliver() → onSuccess().\nviewmodel/BaseApiViewModel.java\nrequest(): Result.onSuccess() → SUCCESS.\nui/ApiActivity.java → attach()\n→ lambda bind(\"create\") nằm trong\nui/user/SendSosActivity.java, onCreate().\nApiActivity.openSos() truyền SOS_ID.",FlowCanvas.Http));
            Right(c,a1,a2,FlowCanvas.Http); Right(c,a2,a3,FlowCanvas.Http);
            float y=Math.Max(a1.Bottom,Math.Max(a2.Bottom,a3.Bottom))+75;
            var setup=Card(c,45,y,690,new Step("B1  TẠO MÀN THEO DÕI","ui/user/TrackSosActivity.java → onCreate()\nui/MapApiActivity.java → setupMapScreen(), initMap().\nui/LiveSosActivity.java → initLiveSos(state): đọc SOS_ID,\nlấy các ViewModel, đăng ký bind(\"detail\" / \"locations\" /\n\"trail\" / \"gps\") và các observer realtime.\nChưa gọi HTTP bằng việc đăng ký bind đơn thuần.",FlowCanvas.Ink));
            var start=Card(c,765,y,690,new Step("B2  ANDROID GỌI onStart()","ui/LiveSosActivity.java → onStart()\n→ realtime.connect()\n→ refreshTask.run() → refreshSos().\nrefreshTask là Runnable lặp mỗi 15 giây.\nrefreshSos() → execute(\"detail\", () -> sosVm.loadDetail(id)).\nWS và tải HTTP không chờ nhau hoàn thành.",FlowCanvas.Ink));
            c.Arrow(new PointF[]{new PointF(CX(a3),a3.Bottom),new PointF(CX(a3),y-30),new PointF(CX(setup),y-30),new PointF(CX(setup),y)},FlowCanvas.Http);
            Right(c,setup,start,FlowCanvas.Muted);
            y=Math.Max(setup.Bottom,start.Bottom)+90;
            c.Text("H  NHÁNH HTTP: TẢI DỮ LIỆU CA",45,y-45,690,36,22,FlowCanvas.Http,true);
            c.Text("W  NHÁNH WS: KẾT NỐI, ĐĂNG KÝ, NHẬN",765,y-45,690,36,22,FlowCanvas.Ws,true);
            var h=Chain(c,45,y,690,new Step[]{
                new Step("H1  HTTP API: CHI TIẾT SOS","ui/ApiActivity.java → execute(\"detail\", ...)\nviewmodel/SosViewModel.java → loadDetail(sosId)\n→ BaseApiViewModel.request()\n→ ApiClient.newCall() + enqueue() [đường đi như A2/A3].\nHTTP GET /api/sos/{id}.",FlowCanvas.Http),
                new Step("H2  CALLBACK CHI TIẾT TRONG UI","ui/ApiActivity.java → attach(): Observer.onChanged()\n→ success lambda bind(\"detail\") được khai báo trong\nui/LiveSosActivity.java → initLiveSos().\nLambda lưu currentSos, kiểm tra ca đã kết thúc; nếu còn\nhoạt động: updateOsrmRoute(), onSosChanged(),\nrenderLiveMap(), realtime.subscribeSos(id) [W2],\ngps.request() khi canPublishGps() [G1], tải H3.",FlowCanvas.Http),
                new Step("H3  HTTP API: VỊ TRÍ HIỆN TẠI VÀ LỊCH SỬ","viewmodel/LocationViewModel.java → loadCurrent(sosId)\nGET /api/location/{id}/current.\nloadTrail(sosId, \"RESCUER\", 100)\nGET /api/location/{id}/trail?actor=RESCUER&limit=100.\nCả hai đều qua BaseApiViewModel.request()\nvà ApiClient.newCall() / enqueue().",FlowCanvas.Http),
                new Step("H4  CALLBACK VỊ TRÍ VÀ LỊCH SỬ","ui/LiveSosActivity.java → initLiveSos():\nlambda bind(\"locations\") → currentSos.setLocations()\n→ updateOsrmRoute() → onSosChanged() → renderLiveMap().\nlambda bind(\"trail\") → cập nhật biến trail\n→ renderLiveMap().\nCác kết quả được chuyển về qua ApiActivity.attach().",FlowCanvas.Http)
            });
            var w=Chain(c,765,y,690,new Step[]{
                new Step("W1  WS CONNECT: MỞ KẾT NỐI","viewmodel/RealtimeViewModel.java → connect() → open()\nopen(): api.url(\"ws\") + query token\n→ api.getHttp().newWebSocket(request, listener) [OkHttp].\nWS /ws?token=...\nWebSocketListener.onOpen() trong open(): CONNECTED,\ngửi lại subscriptions; observer CONNECTED ở Live cũng\ngọi refreshSos() để tải HTTP lại.",FlowCanvas.Ws),
                new Step("W2  WS GỬI: ĐĂNG KÝ CA SOS","H2 gọi viewmodel/RealtimeViewModel.java\nsubscribeSos(sosId)\n→ network/RealtimeProtocol.java → subscribe(sosId)\n→ tạo JSON type=SUBSCRIBE_SOS, sos_id\n→ RealtimeViewModel.send() → socket.send() [SDK].\nNếu chưa CONNECTED: giữ subscription, onOpen() gửi sau.\nconnect() mở kết nối; subscribeSos() chọn ca cần theo dõi.",FlowCanvas.Ws),
                new Step("W3  WS NHẬN: CHUYỂN THÀNH LIVE DATA","viewmodel/RealtimeViewModel.java → open():\nWebSocketListener.onMessage(webSocket, text)\n→ dispatch() → network/RealtimeProtocol.java → decode()\n→ quay lại onMessage(): messages.setValue(message).\nui/LiveSosActivity.java → initLiveSos():\nrealtime.getMessages().observe(this, lambda) nhận kết quả.\nKiểm tra message.sosId đúng ca và ca chưa kết thúc.",FlowCanvas.Ws),
                new Step("W4  OBSERVER PHÂN LOẠI SỰ KIỆN","ui/LiveSosActivity.java → initLiveSos(): observer messages.\nLOCATION_BROADCAST → tạo model/LocationCurrent.java\n→ cập nhật victim / rescuer theo actor\n→ updateOsrmRoute(), onSosChanged(), renderLiveMap().\nSOS_ACCEPTED / STATUS_CHANGED / sự kiện phù hợp khác\n→ refreshSos() → quay lại H1 bằng HTTP.\nSOS_COMPLETED / SOS_CANCELLED → endTracking() [E2].",FlowCanvas.Ws)
            });
            c.Arrow(new PointF[]{new PointF(CX(start),start.Bottom),new PointF(CX(start),y-70),new PointF(CX(h[0]),y-70),new PointF(CX(h[0]),y-52)},FlowCanvas.Http);
            c.Arrow(CX(start),start.Bottom,CX(start),y-52,FlowCanvas.Ws);
            float bottom=Math.Max(h[h.Length-1].Bottom,w[w.Length-1].Bottom);
            y=bottom+80;
            Section(c,y,"G  NHÁNH GPS: GỬI VỊ TRÍ QUA HTTP; WS LÀ CHIỀU NHẬN SỰ KIỆN",FlowCanvas.Gps);
            var g1=Card(c,45,y+70,690,new Step("G1  GPS THIẾT BỊ → publishGps()","util/DeviceLocation.java → request() → start()\n→ client.requestLocationUpdates() [SDK].\nLocationCallback.onLocationResult(): latest = location\n→ onLocation.accept(location).\nCallback được tạo trong LiveSosActivity.initLiveSos():\nthis::publishGps → ui/LiveSosActivity.java → publishGps().",FlowCanvas.Gps));
            var g2=Card(c,765,y+70,690,new Step("G2  HTTP API: CẬP NHẬT VỊ TRÍ","ui/LiveSosActivity.java → publishGps(location):\nkiểm tra canPublishGps(); giới hạn gửi tối thiểu 6 giây.\n→ ApiActivity.execute(\"gps\", ...)\n→ viewmodel/LocationViewModel.java → updateLocation()\n→ BaseApiViewModel.request() → ApiClient.newCall()/enqueue().\nHTTP POST /api/location/{id}.\nWS LOCATION_BROADCAST được xử lý ở W3 → W4.",FlowCanvas.Http));
            Right(c,g1,g2,FlowCanvas.Gps);
            y=Math.Max(g1.Bottom,g2.Bottom)+80;
            Section(c,y,"D  TÍNH TUYẾN ĐƯỜNG VÀ HIỂN THỊ KẾT QUẢ",FlowCanvas.External);
            var d1=Card(c,45,y+70,450,new Step("D1  LẤY HAI VỊ TRÍ","ui/LiveSosActivity.java\nupdateOsrmRoute():\nSosMapData.victim(currentSos),\nSosMapData.rescuer(currentSos).\nKiểm tra valid(lat,lng).\nOsrmRouter.getCached(): có cache\nthì setOsrmRoute(); chưa có thì D2.\nui/rescuer/RealtimeTrackingActivity.java\nfallbackRescuerLocation(): GPS thiết bị.",FlowCanvas.External));
            var d2=Card(c,525,y+70,450,new Step("D2  HTTP NGOÀI: OSRM","util/OsrmRouter.java\nfetchRoute(): HTTP.newCall().enqueue().\nGET router.project-osrm.org\n/route/v1/driving/lng1,lat1;lng2,lat2.\nCallback.onResponse() →\nparseOsrmResponse() → deliver().\nCallback trong Live.updateOsrmRoute():\nmodel/SosRequest.java → setOsrmRoute()\n→ onSosChanged() + renderLiveMap().",FlowCanvas.External));
            var d3=Card(c,1005,y+70,450,new Step("D3  CẬP NHẬT CHỮ VÀ MAP","ui/user/TrackSosActivity.java\nonSosChanged(): chữ, trạng thái, nút.\nui/LiveSosActivity.java\nrenderLiveMap(): kiểm tra mapReady\n→ util/SosMapData.java → draw(),\naddMarker(): marker, tuyến / trail /\nđường thẳng. util/MapHelper.java:\ncreatePinIcon(), moveCamera(),\nfitRouteBounds(). SDK: addPolyline().",FlowCanvas.Ink));
            Right(c,d1,d2,FlowCanvas.External); Right(c,d2,d3,FlowCanvas.Ink);
            y=Math.Max(d1.Bottom,Math.Max(d2.Bottom,d3.Bottom))+80;
            Section(c,y,"E  TẢI LẠI, RỜI MÀN HÌNH VÀ KẾT THÚC CA",FlowCanvas.Muted);
            var e1=Card(c,45,y+70,690,new Step("E1  POLLING VÀ onStop()","ui/LiveSosActivity.java → refreshTask.run()\n→ refreshSos() → postDelayed(this, 15000).\nonStop(): fresh=false, gỡ callback polling\n→ RealtimeViewModel.disconnect()\n→ LocationViewModel.cancelRequests() (BaseApiViewModel).\nutil/DeviceLocation.java → onStop(): gỡ GPS listener.\nui/MapApiActivity.java → onStop(): mapView.onStop().",FlowCanvas.Muted));
            var e2=Card(c,765,y+70,690,new Step("E2  CA KẾT THÚC","ui/LiveSosActivity.java → endTracking()\n→ gỡ polling → DeviceLocation.stop()\n→ LocationViewModel.cancelRequests()\n→ RealtimeViewModel.disconnect() → map.clear()\n→ onSosEnded() được override tại\nui/user/TrackSosActivity.java → onSosEnded()\n→ ApiActivity.openSos(SosResultActivity, id) → finish().",FlowCanvas.Ws));
            y=Math.Max(e1.Bottom,e2.Bottom)+65;
            var actions=Card(c,45,y,690,new Step("THAO TÁC NGƯỜI DÂN CŨNG QUA HTTP","ui/user/TrackSosActivity.java → onCreate(): các listener\n→ ApiActivity.execute() → viewmodel/SosViewModel.java.\nupdateVisibility(): PUT /api/sos/{id}/visibility.\ncancelSos(): POST /api/sos/{id}/cancel\n(sau NavigationHelper.showConfirmSheet()).\nCallback bind visibility / cancel được viết trong onCreate().",FlowCanvas.Http));
            var rescue=Card(c,765,y,690,new Step("PHÍA CỨU HỘ: FILE VÀ CÁC HÀM GỌI HTTP","viewmodel/RescuerViewModel.java\nacceptSos(): POST /api/rescuer/sos/{id}/accept.\nupdateMissionStatus(): POST /api/rescuer/sos/{id}/status.\ncompleteSos(): POST /api/rescuer/sos/{id}/complete.\nUI: RescuerHomeActivity / SosDetailActivity → nhận ca;\nRealtimeTrackingActivity → trạng thái;\nConfirmCompleteActivity → hoàn thành (ui/rescuer/).",FlowCanvas.Http));
            float end=Math.Max(actions.Bottom,rescue.Bottom)+75;
            c.Text("Callback lambda không có tên riêng: tìm trong hàm chứa nó. File và hàm ghi trong mỗi ô là vị trí cần mở để lần code.",45,end-35,1410,45,18,FlowCanvas.Muted);
            Save(c,path,end+35);
        }
    }
}
'@

Add-Type -TypeDefinition $diagramSource -ReferencedAssemblies System.Drawing
[DetailedFlowDiagrams]::Map((Join-Path $PSScriptRoot 'luong-map-chi-tiet.png'))
[DetailedFlowDiagrams]::Sos((Join-Path $PSScriptRoot 'luong-sos-chi-tiet.png'))
