import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;



public class client_and_server {

	static File dir = new File("user.dir");
	static String dirway;
	static int Userport; //kullanıcının mesajları kabul ettiği port numarası
	static String username;//kullanıcının özgün takma adı
	static String desname;//kullanıcın mesaj atmak istediği kişinin takma adı
	static boolean isconnected;//kullanıcının bir başkasına bağlı olup olmadığını kontrol eden flag
	static String command="?*?";//kullanıcıdan alınan komutlardan biri
	static String command2="?*?";//kullanıcıdan alınan komutlardan biri
	static int status = 0;// 0 ise bağlantı kabul edebilir, 1 ise başkasına mesaj gönderiliyor, 2 ise mesaj alınıyor.
	public static BufferedReader br=new BufferedReader(new InputStreamReader(System.in));// klavye girdilerini okuyacak input stream
	static File eskimesajlar,liste,gruplar;// eski mesajların, kullanıcı listesinin ve grupların tutulduğu,
																				// txt dosyalarına ulaşmak için açılan File objeleri.
	static Writer out,out2,out3,out4;// eski mesajların, kullanıcı listesinin ve grupların tutulduğu,
																		// txt dosyalarına veri yazmak için açılan Writer objeleri.
	static Scanner scan,scan2,scan3;// eski mesajların, kullanıcı listesinin ve grupların tutulduğu,
																		// txt dosyalarından veri okumak için açılan Scanner objeleri.
	static SimpleDateFormat tarihFormati = new SimpleDateFormat("dd MMMM yyyy hh:mm:ss", new Locale("tr"));
	// Mesajlari damgalamak ve zamanı almak için kullanılan obje
	static String zaman;// zamanın yazıldığı string
	private static Scanner scan4;// özel işler için kullanılacak scanner objesi


	public static void main(String args[]) throws Exception{
		dirway = dir.getAbsolutePath();
		if(dirway.contains("user.dir")){
    		dirway = dirway.replaceAll("user.dir", "");// kullanıcının yolunun sonundaki "user.dir" yazısı silinir
    	}
		System.out.println("Mesajları kabul etmek istediğiniz portu giriniz: ");// kullanıcıdan Mesajları kabul etmek istediği port alınır
		Userport = Integer.parseInt(br.readLine());
		System.out.println("Kullanıcı adı giriniz: ");// kullanıcıdan takma adı alınır
		username = br.readLine().trim();
		eskimesajlar = new File(dirway+"/"+username+"_mesajlar.txt");// eski mesajların kaydedileceği txt dosyası açılır
		liste = new File(dirway+"/"+username+"_liste.txt");// kullanıcı listesinin bulunduğu ve kaydedileceği txt dosyası açılır
																											// kullanıcı listesinin bulunduğu username_liste.txt dosyasında kullanıcılar "username#IP#PortNumber#" şeklinde saklanır
		gruplar = new File(dirway+"/"+username+"_gruplar.txt");// kullanıcının gruplarının bulunduğu txt dosyası açılır
																													// username_gruplar.txt dosyası kullanıcıya ait grupların isimlerini barındırır
																													// ve grubunismi_grubu.txt ise o gruptaki kayıtlı kullanıcıların bilgisini barındırır.
		out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(eskimesajlar,true), "UTF8"));// eski mesajların kaydedileceği txt dosyasına veri yazman için Writerstream başlatılır.
		scan = new Scanner(eskimesajlar);
		out2 = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(liste,true), "UTF8"));// kullanıcı listesinin kaydedileceği txt dosyasına veri yazman için Writerstream başlatılır.
		scan2 = new Scanner(liste);
		out3 = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(gruplar,true), "UTF8"));// kullanıcının gruplarının kaydedileceği txt dosyasına veri yazman için Writerstream başlatılır.
		scan3 = new Scanner(gruplar);
		if(!eskimesajlar.exists()){
			eskimesajlar.createNewFile();// eğer kullanıcı yeni bir kullanıcı ise ona özel yeni gerekli dosyalar kullanıcı ismiyle açılır
		}
		if(!liste.exists()){
			liste.createNewFile();// eğer kullanıcı yeni bir kullanıcı ise ona özel yeni gerekli dosyalar kullanıcı ismiyle açılır
		}
		if(!gruplar.exists()){
			gruplar.createNewFile();// eğer kullanıcı yeni bir kullanıcı ise ona özel yeni gerekli dosyalar kullanıcı ismiyle açılır
		}



		new ServerHandler(Userport, username).start();// bu kullanıcıya bir başkasından gelen mesajları kontrol etmesi için yeni thread oluşturulur
		while(true){
			try {
				System.out.println("Eğer başkasından mesaj beklemek istemiyorsanız menüye erişmek için '1' e basınız.");

				command = br.readLine().trim();
				command2=command;// bir başka kullanıcının mesaj atması haline yukarıdaki beklenen klavye girdisini iptal etmek için kullanılır.
				if(command.compareTo("1")==0 && status!=2){// eğer kullanıcı 1 girerse menüye geçilir ve başka kullanıcıladan gelen mesajlar kabul edilmez bu süre içinde
					status =1;// status bir demek menüde veya başkasına mesaj gönderiyor demek
					displayMenu();// menü gösterme fonksiyonu
				}
				else{
					while(status == 2){
						Thread.sleep(100);// eğer bir başkasından mesaj alınıyorsa bu serverthread de işlem görür
						// bu yüzden main threahdinin 72. satıra dönüp klavye girdilerini karıştırmaması için
						// status 2 olduğu sürece main threadinde loop da dönülür
					}
				}
			} finally {
				status=0;
			}
		}


	}
	// uygulama açılır açılmaz gelicek mesaj isteklerine cevap vermek için serverhandler threadi
	// kullanıcının girişte girdiği port numarasını kullanarak bir serversocket oluşturur
	// server handler threadi bu serversockete bağlanmak isteyenleri kabul eder ve yeni ServerThread objesi oluşturur.
	// mesajların alınıp gönderilmesi işine ServerThread bakar.
	// ServerHandler sadece gelen istekleri kabul eder ve yeni ServerThread objeleri oluşturur.
	static class ServerHandler extends Thread{
		ServerSocket ssoc;
	    public ServerHandler(int port, String name) throws IOException {
	    	ssoc = new ServerSocket(port);// server socket objesi başlatılır
	    }

	    public void run(){
	    	try {
	            while (true) {// serverhandler thread sürekli olarak yeni bir istemci bekler.
	            	// Yeni istemci geldiğinde, yeni bir thread ile ServerThread objesi oluşturulur
									try {
										new ServerThread(ssoc.accept());
									} catch (IOException e) {
										// TODO Auto-generated catch block
										System.out.println("Bağlantı kabul edilemedi");
								}
	            }
			} finally {
	        	// Sunucu kapatılırken
	        	try {
					ssoc.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	    }

	}
	static class ServerThread extends Thread{
		Socket nsoc;
		DataInputStream din;// soketten gelen veriler bu streamle alınır.
	    DataOutputStream dout;// sokete bu stream kullanılarak veri aktarılır.

		public ServerThread(Socket soc) throws IOException {
			if(status != 0){// eğer status sıfır değil ise ve biri mesaj atmaya çalıştıysa, o kişiye meşgul mesajı gönderilir ve thread geri kapatılır
				nsoc = soc;// ServerHandlerın kabul ettiği socket locak değişkene atılır
				din=new DataInputStream(nsoc.getInputStream());// getInputStream atanır.
	            dout=new DataOutputStream(nsoc.getOutputStream());// getOutputStream atanır.
	            dout.writeUTF("MESGUL");
	            this.start();
			}
			else{// eğer status 0 ise mesajlaşmak için bağlanmaya çalışan diğer istemci kabul edilir gerekli düzenlemeler yapılır mesajlaşma başlatılır
				status=2;// status 2 demek bie istemcinin bu kullanıcı ile mesajlaşma başlattı demektir.
				nsoc = soc;
				din=new DataInputStream(nsoc.getInputStream());// getInputStream atanır.
	            dout=new DataOutputStream(nsoc.getOutputStream());// getOutputStream atanır.
	            // yeni kullanıcı ismi, hangi IP ve porttan bağlandığı bilgisi loglanır.
	            dout.writeUTF("HAZIR");// karşı tarafa meşgul olunmadığı bilgisi gönderilir
	            String firstInfo;
	            firstInfo = din.readUTF();// karşı tarafın takma ad bilgisi alınır
	            desname = firstInfo;
	            dout.writeUTF(username);// karşı tarafı bu kullanıcının takma ad bilgisi gönderilir
	            System.out.println(firstInfo+" adlı kullanıcı"+ nsoc.getInetAddress()+" adresinden "+nsoc.getPort()+" nolu porttan sizinle konuşma başlattı.");// bağlantı console'a yazılır
	            out.append(tarihFormati.format(new Date())+ " - "
	            		+firstInfo+" adlı kullanıcı"+ nsoc.getInetAddress()+" adresinden "+nsoc.getPort()+" nolu porttan sizinle konuşma başlattı.").append("\r\n");
	            out.flush();// bu kullanıcının eski mesajlar text dosyasına kimin nezaman hangi IP ve porttan bağlandığı bilgisi yazılır
	            scan2=new Scanner(liste);// scan2 yeniden başlatılırki liste dosyasının başına gidilsin
	            scan2.useDelimiter("#|\\r\\n");// kullanıcı listesinin bulunduğu username_liste.txt dosyasında kullanıcılar "username#IP#PortNumber#" şeklinde saklanır
	            if(scan2.findWithinHorizon(desname, (int)liste.length())==null)// kullanıcı listesinde mesajlaşmayı başlatan kişinin takma adı için arama yapılır
	            {																															// eğer takma ad kullanıcı listesinde zaten bulunuyorsa yeniden eklenmez
	            	out2.append(desname+"#"+nsoc.getInetAddress()+"#"+nsoc.getPort()).append("\r\n");
	            	out2.flush();
	            }
	            this.start();
			}

		}

		public void run(){
			try {
				if(status ==2){// status 2 ise girilir.
					isconnected=true;// mesajlaşmanın devam ettiği bilgisini tutan flag true yapılır
					ServerSender sender = new ServerSender(nsoc, dout);// aynı anda hem mesaj gönderip hemde mesaj alabilmek için, mesaj gönderme işlemi ServerSender adındaki başka bir thread ile yapılır

					String rcv;// karşı taraftan gelen mesajları tutacak string
			    	int timeout=0;// bağlantının alışılmadık şekilde düşmesi halinde timeout değeri kontrol edilerek bu bağlantı kesintisi algılanır
			    	try {
			    		String scc= din.readUTF();//karşı taraftan "OK" bilgisi alınır
			    		//System.out.println("scc: "+ scc);
					} catch (IOException e1) {
						e1.printStackTrace();
					}

			    	try {
			    		while(timeout<4 && isconnected){// eğer isconneted flagi true ve timeout değeri 4 ten küçükse mesajlaşmaya devam edilir
				    		timeout++;
			    			rcv = din.readUTF();// karşı kullanıcının gönderdiği mesaj alınır
			    			if(rcv!=null){// eğer mesaj null değilse timeout değeri sıfırlanır
			    				timeout=0;
			    				System.out.println(desname+": "+ rcv);// karşı taraftan gelen mesaj karşı tarafın takma adıyla console'a basılır
			    				out.append(tarihFormati.format(new Date())+ " - "+ desname +": "+rcv).append("\r\n");// gelen bu mesaj username_list.txt dosyasınada zaman bilgisi ile kaydedilir.
			    				out.flush();
			    			}
			    			if(rcv.compareTo("&")==0){// eğer karşı taraf '&' karakterini yolladıysa bu mesajlaşmanın bitirilmek istendiği anlamına gelir ve bunun için bağlantılar kesilir ve beklemeye geri dönülür.
			    				System.out.println("Geri dönmek için herhangi bir tuşa basınız");
			    				isconnected=false;
			    				timeout=10;
			    				continue;
			    			}
				    	}
			    		if(!nsoc.isClosed()){// eğer karşı tarafın bağlantısının tutulduğu soket hala açık kaldıysa kapatılır
				    		try {
								nsoc.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
				    	}
			    		out.append(tarihFormati.format(new Date())+ " - "+ desname +" kullanıcısı ile bağlantı kesildi.").append("\r\n");// karşı tarafla bağlantının nezaman kesildiği username_list.txt dosyasına kaydedilir
			    		out.flush();
					} catch (Exception e) {
						// TODO: handle exception
					}
				}

			} catch (Exception e) {
				e.getLocalizedMessage();
			} finally {
				System.out.println(desname+" kullanıcısı ile bağlantı kesildi.");

				status=0;// son olarak en eski durum olan idle duruma geçmek için status sıfırlanır bu sayede yeni bağlantılar kabul edilebilir.
			}

		}

	}
	// ServerSender threadi sadece ServenHandler threadinin kabul ettiği bağlatıya klavyeden girilen girdileri göndermek için oluşturulmuştur.
	// ServerSender ve ServerThreadin multi-thread olarak aynı anda çalışması sayesinde, aynı anda hem mesaj alınabilir hemde mesaj atılabilir.
	static class ServerSender extends Thread{
		Socket nsoc;
		DataOutputStream dout;// getOutputStream atanır.

	    public ServerSender(Socket soc, DataOutputStream out) throws IOException{
	    	nsoc=soc;
	    	dout=out;
	    	dout.writeUTF("BASLAT");// karşı tarafa mesaj gönderen threadin hazır olduğu bilgisi iletilir
	    	start();
	    }

	    public void run(){
	    	if(command.compareTo(command2)==0){// main metodundaki klavye girdisi bekleyen İnputstreamin, kullanıcının göndermek istediği mesajı karıştırmaması için alınmış bir önelemdir.
					 //Kullanıcı en az bir kez klavyeden 'ENTER' tuşuna basmalıdır bu sayede bir sonraki girdileri karşı tarafa gönderilebilir
	    		System.out.println(desname +" kullanıcısı ile konuşmaya başlamak için herhangi bir tuşa basınız.");
	    		while(command.compareTo(command2)!=0){
	    			try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		}
	    	}
				// Konuşma resmi olarak başlar, eğer '&' girilirse konuşma sonlandırılır.
	    	System.out.println(desname+" kullanıcısı ile artık mesajlaşablirsiniz. Konuşmayı sonlandırmak isterseneiz sadece '&' karakterini giriniz.");
	    	try {
	    		while(isconnected){
						System.out.println("-> ");
						String sending = br.readLine().trim();// kullanıcıdan klavye verisi alınır
					if(isconnected){// eğer bağlantı hala hayattaysa mesaj gönderilir
						dout.writeUTF(sending);
						out.append(tarihFormati.format(new Date())+ " - "+ username +": "+sending).append("\r\n");// gönderilen mesaj username_liste.txt dosyasına zaman bilgisiyle kaydedilir.
						out.flush();
					}

					if(sending.compareTo("&")==0){// eğer '&' karakteri geldiyse konuşmayı sonlandırmak while looptan çıkılır
						isconnected=false;
					}

				}
				nsoc.close();
				System.out.println("sender kapatıldı.");
			} catch (Exception e) {
			}
	    }
	}

	// Kullanıcı bir başka kullanıcıya bağlanmaya çalıştığında ClientThread threadi çalıştırılır.
	// ClientThread threadi mesajlaşmayı başlatırken aynı zamanda ClientReciver adında karşı taraftan gelecek mesajları aln bir başka thread de başlatır.
	// ClientThread threadi ise mesajlaşma sırasında sadece mesaj gönderme işlemi yapar, bu sayede mesajlar aynı anda hem alınabilir hemde gönderilebilir.
	static class ClientThread extends Thread{
		Socket nsoc;
		DataInputStream din;// soketten gelen veriler bu streamle alınır.
	    DataOutputStream dout;// sokete bu stream kullanılarak veri aktarılır.
	    InetAddress address;// klavyeden gelicek komutlar bu reader ile okunur.

	    public ClientThread(Socket soc) throws IOException {
	    	nsoc = soc;
	    	din=new DataInputStream(nsoc.getInputStream());// getInputStream atanır.
            dout=new DataOutputStream(nsoc.getOutputStream());// getOutputStream atanır.
            start();
	    }

	    public void run(){
	    	String mess;// mesajlaşma başlamadan önce gerekli bağlantı bilgilerini gönderecek string
	    	try {
    			mess =din.readUTF();// Konuşma başlatılacak kişinin meşgul olup olmadığı bilgisi alınır
	    		if(mess.compareTo("MESGUL")==0){// eğer meşgulse bilgi console'a yazılır ve bağlantı kapatılıp menüye dönülür
	    			System.out.println("Mesajlaşmak istediğiniz kullanıcı müsait değil."+
	    		" Daha sonra tekrar deneyiniz.");
	    		}
	    		else if(mess.compareTo("HAZIR")==0){// eğer konuşma başlatılacak kişi meşgul değilse mesajlaşma için gerekli bilgiler alışveriş edilir.
	    			dout.writeUTF(username);// ilk olarak bağlanmaya çalışan taraf takma adını gönderir
	    			desname=din.readUTF();// daha sonra karşı tarafın takma adı alınır
	    			isconnected=true;// bağlantının canlı olup olmadığını gösteren flag true yapılır
	    			ClientReciver reciver = null;// gelicek mesajları alacak ClientReciver threadi oluşturulur
						// konuşmanın başladığı ve nasıl sonlandırılacağı console'a yazılır
	    			System.out.println(desname+" kullanıcısı ile artık mesajlaşablirsiniz. Konuşmayı sonlandırmak isterseneiz sadece '&' karakterini giriniz.");
	    			out.append(tarihFormati.format(new Date())+ " - "
		            		+desname+" adlı kullanıcısı ile"+ nsoc.getInetAddress()+" adresinden "+nsoc.getPort()+" portundan konuşma başlatıldı.").append("\r\n");
	    			out.flush();// Kiminle nezaman konuşma başlatıldığı username_liste.txt dosyasına kaydedilir
	    			scan2=new Scanner(liste);// scan2 yeniden initiate edilirki dosyanın başına dönülsün
	    			if(scan2.findWithinHorizon(desname, (int)liste.length())==null)// eğer bağlanılan kullanıcı listede yok ise kullanıcı listesine eklenir
		            {
		            	out2.append(desname+"#"+nsoc.getInetAddress()+"#"+nsoc.getPort()+"#").append("\r\n");
		            	out2.flush();
		            }
    				reciver = new ClientReciver(nsoc, din);// ClientReciver threadi initiate edilir
    				dout.writeUTF("BASLAT");// karşı tarafa her şeyin tamam olduğu bilgisi yollanır
	    			while(isconnected){
	    				System.out.println("-> ");
	    				String sending = br.readLine().trim();// kullanıcının göndereceği mesaj klavyeden alınır
	    				if(isconnected){
	    					dout.writeUTF(sending);// mesaj gönderilir
	    					out.append(tarihFormati.format(new Date())+ " - "+ username +": "+sending).append("\r\n");// gönderilen mesaj username_liste.txt dosyasına zaman bilgisi ile kaydedilir
	    					out.flush();
	    				}

	    				if(sending.compareTo("&")==0){// eğer '&' karakteri geldiyse while looptan çıkılır ve bağlantı sonlandırılır
	    					isconnected=false;
	    				}
	    			}
	    			nsoc.close();// bağlatı sonlandırılırken socket kapatılır
	    			reciver.join();// ClientReciver threadininde ölmesi beklenir
						// bağlantının tam olarak nezaman ve kiminli koptuğu username_liste.txt dosyasına kaydedilir
	    			out.append(tarihFormati.format(new Date())+ " - "+ desname +" kullanıcısı ile bağlantı kesildi.").append("\r\n");
	    			out.flush();
	    		}


			} catch (Exception e) {
				e.getLocalizedMessage();
			}
	    }

	}
	//ClientReciver threadi ClientThread threadinden başlatılır ve mesajlaşma sırasında karşıdan gelen mesajların alınmasından sorumludur.
	//ClientReciver threadi sayesinde konuşma sırasında aynı anda mesaj gönderilip alınablir.
	static class ClientReciver extends Thread{
		Socket nsoc;
		DataInputStream din;// soketten gelen veriler bu streamle alınır.
	    public ClientReciver(Socket soc, DataInputStream in){
	    	nsoc=soc;
	    	din=in;
	    	start();
	    }

	    public void run(){
	    	String rcv;// karşı taraftan gelen mesajları tutacak string
	    	int timeout=0;// bağlantının alışılmadık şekilde düşmesi halinde timeout değeri kontrol edilerek bu bağlantı kesintisi algılanır
	    	try {
	    		String scc= din.readUTF();//karşı taraftan "OK" bilgisi alınır
	    		//System.out.println("scc: "+ scc);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	    	while(timeout<4 && isconnected){// eğer isconneted flagi true ve timeout değeri 4 ten küçükse mesajlaşmaya devam edilir
	    		try {
	    			timeout++;// her while loopunda timeout 1 arttırılır
	    			rcv = din.readUTF();// karşı kullanıcının gönderdiği mesaj alınır
	    			if(rcv!=null){// eğer mesaj null değilse timeout değeri sıfırlanır
	    				timeout=0;
	    				System.out.println(desname+": "+ rcv);// karşı taraftan gelen mesaj karşı tarafın takma adıyla console'a basılır
	    				out.append(tarihFormati.format(new Date())+ " - "+ desname +": "+rcv).append("\r\n");// gelen bu mesaj username_list.txt dosyasınada zaman bilgisi ile kaydedilir.
	    				out.flush();
	    			}
	    			if(rcv.compareTo("&")==0){// eğer karşı taraf '&' karakterini yolladıysa bu mesajlaşmanın bitirilmek istendiği anlamına gelir ve bunun için bağlantılar kesilir ve beklemeye geri dönülür.
	    				isconnected=false;
	    				timeout=10;
	    				continue;
	    			}
				} catch (Exception e) {

				}
	    	}
	    	isconnected=false;
	    	System.out.println(desname+" kullanıcısı ile bağlantı kesildi.");

	    	if(!nsoc.isClosed()){// eğer karşı tarafın bağlantısının tutulduğu soket hala açık kaldıysa kapatılır
	    		try {
					nsoc.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    }
	}
	// displayMenu() metodu Kullanıcıya uygulamanın MENU arayüzünde gezinmeyi sağlar
	public static void displayMenu() throws Exception
	{
		while(status==1)// Kullanıcıdan komutlar burada alınır ve yönlendirilir.
        {
        	System.out.print("\n============================\n");
            System.out.println("[ MENU ]");
            System.out.println("1. Yeni Sohbet Başlat");
            System.out.println("2. Kullanıcı Listesi ve Kullanıcı Gruplar");
            System.out.println("3. Eski mesajlar");
            System.out.println("4. Menüden Çık ve Başkasından Mesaj Bekle");
            System.out.println("5. Uygulamayı Kapat");
            System.out.print("\nKomut Giriniz :");
            int choice;
            try{
            	choice=Integer.parseInt(br.readLine());
            } catch (Exception e) {
            	System.out.print("Lütfen bir sayı giriniz.");
            	continue;
			}
            if(choice==1)// '1' girilirse kullanıcının el ile IP ve Port girilmesi istenir ve bu bilgilerden yeni bir sohber başlatılır
            {
            	System.out.println("Mesaj göndermek istediğiniz kişinin IP sini ve Portunu giriniz.\n IP: ");
            	String desip = br.readLine().trim();
            	System.out.println("Port: ");
            	int desport = Integer.parseInt(br.readLine());
            	connectsomeone(desip, desport);
            }
            else if(choice==2)// '2' girilirse kullanıcıya yeni bir menü gösterilir bu menüde kullanıcı kendisine ait gruplarını ve kayıtlı kullanıcı listesini görüntüleyebilir
            {									// ve görüntülenen kullanıcılardan birisinin sadece takma adı kullanılarak yeni sohbet başlatılabilir
            	boolean cıkıs =true;// aşağıdaki while loopunu kontrol eden flag
            	while(cıkıs){
            		String option;
                	System.out.print("\n============================\n");
                	System.out.println("1. Tüm Kullanıcı Listesini göster");
                	System.out.println("2. Kullanıcı Gruplarını Göster");
                	System.out.println("3. Yeni Grup Oluştur");
                	System.out.println("4. Ana Menüye Dön");
                	System.out.print("\nKomut Giriniz :");
                	option = br.readLine().trim().toLowerCase();
                	if(option.compareTo("1")==0){// '1' girilirse kullanıcıya kayıtlı kişilerin listesi gösterilir
																							// ve bu kayıtlı kişilerden birinin sadece ismi kullanılarak yeni bir sohbet oluşturulabilir
                		scan2 = new Scanner(liste);// username_liste.txt dosyasının başına dönmek için scan2 yeniden initiate edilir.
                		int counter =0;
                		while(scan2.hasNextLine()){// dosyanın sonuna kadar bütün kayıtlı kişiler yazdırılır
                			counter++;
                			System.out.println(counter+". "+scan2.nextLine());
                		}
                		System.out.println("Mesaj göndermek istediğiniz birisi varsa kullanıcı adını giriniz giriniz.\n"+
                		"Menüye dönmek için 'x' griniz.");
                		option = br.readLine().trim();
                		if(option.compareTo("x")!=0){//eğer kullanıcı 'x' harici bir şey girerse uygulama girdiyi bir isim olarak algılar ve
																								// bu girdiyi username_liste.txt dosyasında arar, eğer bulursa bu kişite ait bilgiler kullanılarak yeni sohbet başlatılmaya çalışılır.
                			scan2 = new Scanner(liste);
                    		scan2.useDelimiter("#|\\r\\n");// username_liste.txt dosyasının başına dönmek için scan2 yeniden initiate edilir.
                    		if(scan2.findWithinHorizon(option, (int)liste.length())!=null){// girdi dosyada aranılır
                    			String desip = scan2.next();//bulunan kişinin ipsi alınır
                    			desip= desip.replaceAll("/", "");// ip adresinin başındaki slash karakteri kaldırılır
                        		int desport = Integer.parseInt(scan2.next());//bulunan kişinin port numarası alınır
                        		connectsomeone(desip, desport);//ip ve port numarası kullanılarak connectsomeone() methodu ile yeni sohbet başlatılmaya çalışılır.
                    		}
                    		else{// girdi dosyada bulunamazsa "böyle biri yok" mesajı basılır.
                    			System.out.println("Böyle bir kullanıcı bulunmamaktadır.");
                    		}

                		}
                		else if(option.compareTo("x")==0){// eğer kullanıcı 'x' girerse menüye dönülür
                			cıkıs=false;
                		}
                	}
                	else if(option.compareTo("2")==0){// '2' girilirse username_gruplar.txt dosyasındaki kişiye ait gruplar listelenir
                		if(gruplar.length()<1){// dosya boşsa "Hiç bir grubunuz bulunmamaktadır." mesajı yazılır.
                			System.out.println("Hiç bir grubunuz bulunmamaktadır.");
                		}
                		else{
                			scan3 = new Scanner(gruplar);
                			while(scan3.hasNextLine()){// username_gruplar.txt dosyasındaki kişiye ait gruplar listelenir
                				System.out.println(scan3.nextLine());
                			}
                			boolean innercıkıs=true;//aşağıdaki while loopa ait flag
                			while(innercıkıs){// bu while loopda kullanıcıya az önce listelenen grupların içerdiği kullanıcılarını listelemek isteyip istemediği sorumludur
																				// ve eğer grubun kullanıcıları listelenirse, sadece takma ad ile herhangi bir kullanıcıya bağlanılabilir veya o gruba yeni bir kullanıcı eklenebilir.
                				System.out.println("Bir grubun kullanıcılarını listelemek istiyorsanız grubun ismini giriniz.");
                    			System.out.println("Geri dönmek istiyorsan 'x' giriniz");
                    			option = br.readLine().trim();
                    			if(option.compareTo("x")!=0){//'x' girilirse bir önceki menüye dönülür
                    				scan3 = new Scanner(gruplar);
                    				File innergrup=new File(dirway+"/"+option+"_grubu.txt");//grubunismi_grubu.txt dosyası açıkır
                    				if(innergrup.exists()){
                            			scan4 = new Scanner(innergrup);//grubunismi_grubu.txt dosyasının içindeki kayıtlı kullanıcılar okunur
                            			int counter =0;
                                		while(scan4.hasNextLine()){
                                			counter++;
                                			System.out.println(counter+". "+scan4.nextLine());//grubunismi_grubu.txt dosyasının içindeki kayıtlı kullanıcılar ekrana yazılır
                                		}
                                		System.out.println("\nMesaj göndermek istediğiniz birisi varsa kullanıcı adını giriniz giriniz.\n"+
                                		"Gruba yeni birini eklemek istiyorsanız '1' giriniz.\n"+"Geri dönmek istiyorsanız 'x' giriniz.");
                                		option = br.readLine().trim();
                                		if(option.compareTo("x")==0){//'x' girilirse bir önceki menüye dönülür
                                			innercıkıs=false;
                                			continue;
                                		}
                                		else if(option.compareTo("1")==0){//'1' girilirse kayıtlı kullanıcıları listelenen gruba yeni kişi ekleme yapılır
                                			String line;
                                			System.out.println("Eklemek istediğiniz kişinin,\n"+"Kullanıcı adı: ");
                                			line = br.readLine().trim();// eklenecek yeni kişinin takma adı alınır
                                			System.out.println("IP Adresi: ");
                                			line = line +"#/"+br.readLine().trim();// eklenecek yeni kişinin ipsi alınır
                                			System.out.println("Port numarası: ");
                                			line = line +"#"+br.readLine().trim()+"#";// eklenecek yeni kişinin port numarası alınır
                                			out4=new BufferedWriter(new OutputStreamWriter(
                                					new FileOutputStream(innergrup,true), "UTF8"));// OutputStreamWriter ile grubunismi_grubu.txt dosyasını yeni kişi eklenir
                                			out4.append(line).append("\r\n");
                                			out4.flush();
                                			System.out.println("Yeni kullanıcı gruba başarıyla eklenmiştir.");
                                		}
                                		else{//'x' veya '1'den farklı bir mesaj gelirse uygulama bunu o gruptaki bir işine mesaj gönderilmeye çalışılmak istendi olarak algılar
																				// ve bu yüzden bu girdiyi bir isim olarak grubunismi_grubu.txt dosyasının içinde arar
                                			scan4 = new Scanner(innergrup);
                                    		scan4.useDelimiter("#|\\r\\n");
                                    		if(scan4.findWithinHorizon(option, (int)innergrup.length())!=null){// eğer isim grupta bulunursa
																																												// connectsomeone() metodu ile bu kişinin kayıtlı bilgileri kullanılarak sohbet başlatılmaya çalışılır.
                                    			String desip = scan4.next();
                                    			desip = desip.replaceAll("/", "");
                                        		int desport = Integer.parseInt(scan4.next());
                                        		connectsomeone(desip, desport);
                                    		}
                                    		else{// eğer isim bulunamazsa "Böyle bir kullanıcı bulunmamaktadır." mesajı ekrana basılır.
                                    			System.out.println("Böyle bir kullanıcı bulunmamaktadır.");
                                    		}
                                		}
                            		}
                            		else{// eğer kullanıcıları listelenmek istenen grup yoksa "Böyle bir grup bulunmamaktadır." mesajı ekrana basılır.
                            			System.out.println("Böyle bir grup bulunmamaktadır.");
                            		}

                    			}
                    			else if(option.compareTo("x")==0){//'x' girilirse menüye dönülür.
                    				innercıkıs=false;
                    			}
                			}

                		}
                	}
                	else if(option.compareTo("3")==0){// '3' girilirse yeni bir grup oluşturmak için kullanıcıdan gerkeli bilgiler alınır.
                		System.out.println("Oluşturmak istediğiniz grubun ismini giriniz.\n"+
                	"Grup Adı: ");
                		option = br.readLine().trim();// oluşturulmak istenen grubun ismi alınır
                		File yeni_grup= new File(dirway+"/"+option+"_grubu.txt");// bu grup grubunismi_grubu.txt adıyla bir text dosyası olarak oluşturulur.
                		if(!yeni_grup.exists()){// eğer grup zaten yoksa oluşturulur
                			yeni_grup.createNewFile();
                			out3.append(option).append("\r\n");
                			out3.flush();
                			System.out.println(option +" grubu oluşturuldu.");
                		}
                		else{// eğer grup varsa bilgi ekrana basılır ve grup yeniden oluşturulurmaz
                			System.out.println("Grup zaten var.");
                		}
                	}
                	else if(option.compareTo("4")==0){// ana meüye geri dönülür
                		cıkıs=false;
                	}
            	}

            }
            else if(choice==3){// '3' gelirse eski mesajlar kullanıcıya listlenir ve kullanıcı isterse bu mesajlar arasında kelime araması yapablir
            	scan = new Scanner(eskimesajlar);
        		while(scan.hasNextLine()){
        			System.out.println(scan.nextLine());
        		}
        		System.out.println("Eski mesajlar arasında aramak istediğiniz bir kelime varsa kelimeyi giriniz.\n"+"Menüye geri dönmek için 'x' giriniz.");
        		String option=br.readLine();
        		if(option.compareTo("x")==0){
        			continue;
        		}
        		else{
        			scan = new Scanner(eskimesajlar);
        			if(scan.findWithinHorizon(option, (int)eskimesajlar.length())==null){
        				System.out.println("Girdiğiniz kelimeyi içeren bir mesaj bulunamadı.");
        			}
        			else{
        				scan = new Scanner(eskimesajlar);
        				boolean tamam=true;
        				while(tamam){
        					String satir;
        					satir=scan.nextLine();
            				if(satir.contains(option)){
            					System.out.println("# "+satir+" #");
            					System.out.println("\n Bu satırın dahada ilerisinde aramak istiyorsanız 'ENTER' tuşuna basınız.\n"+"Menüye dönmek istiyorsanız 'x' giriniz");
            					String option2=br.readLine();
                				if(option2.compareTo("x")==0){
                					tamam=false;
                				}

            				}
            				if(!scan.hasNextLine() && tamam==true){
        						System.out.println("Aradığınız kelimeden başka bulunamadı.");
        						tamam=false;
        					}

        				}

        			}
        		}
            }
            else if(choice==4)// Menüden çıkılır.
            {
            	status=0;
            	break;
            }
            else if(choice == 5)// Uygulamaya çıkış emri verilir.
            {
                System.exit(1);
            }
        }
	}

	// connectsomeone() metodu bir ip adresi ve bir port numarası alır ve yeni ClientThread oluşturarak bu bilgilerle yeni bir sohbet başlatmaya çalışır.
	public static void connectsomeone(String ip,int portnumber){
		try {
			String desip= ip;
			int desport=portnumber;
			Socket dsoc=new Socket(desip, desport);
	    	ClientThread ncon = new ClientThread(dsoc);
	    	ncon.join();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
