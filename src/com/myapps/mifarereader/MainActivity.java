package com.myapps.mifarereader;

import java.io.IOException;
import java.math.BigInteger;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{

	private static final String TAG = null;
	TextView mTextView;
	EditText mSector; 
	EditText mKey;
	TextView mResult;
	Button mReadTagButton;
	Button mReadSectorButton;
	CheckBox mDefault;
	private NfcAdapter mAdapter;
	private boolean mInReadMode;
	private boolean default_key;
	
	private ListView mListView ;
    private ArrayAdapter<String> mArrayAdapter ;
    
    MifareClassic mfc;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mSector = (EditText)findViewById(R.id.sec_no);
        mKey = (EditText)findViewById(R.id.keya);
        mResult = (TextView)findViewById(R.id.result);
        mReadTagButton = (Button)findViewById(R.id.read_tag_button);
        mReadSectorButton = (Button)findViewById(R.id.read_sector_button);
        mReadSectorButton.setVisibility(View.GONE);
        mReadTagButton.setOnClickListener(this);
        mReadSectorButton.setOnClickListener(this);
    
        addListenerOnmDefault();
        
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        default_key=false;
	}
	
	public void onClick(View v) {
		if(v.getId() == R.id.read_tag_button) {
			displayMessage("Touch and hold tag against phone to read.");
			enableReadMode();
		}
		if(v.getId() == R.id.read_sector_button) {
			setContentView(R.layout.reader_activity);

	        mArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
	        mListView = (ListView) findViewById(R.id.in);
			 mListView.setAdapter(mArrayAdapter);
			
			int sector = getSector();
			
			mArrayAdapter.add("Auth successful with KeyA");
			//mListView.setBackgroundColor(Color.WHITE);
			mArrayAdapter.add("Reading sector : "+sector);
			
			int bCount = mfc.getBlockCountInSector(sector);
            int bIndex = 0;
            byte[] data = null;
            String value;

            bIndex = mfc.sectorToBlock(sector);
            for(int i = 0; i < bCount; i++){

            	//mListView.setBackgroundColor(Color.GREEN);
                mArrayAdapter.add("Reading block : "+i);

                // 6.3) Read the block
                try {
					data = mfc.readBlock(bIndex);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                value = new String(data);
                
                if(i==3)
                value = hex(value);
                
                //mListView.setBackgroundColor(Color.BLUE);
                mArrayAdapter.add(value);
                bIndex++;
            }
            
		}
	}
	
	public void displayMessage(String s)
	{
		mResult.setText(s);
	}
	
	@Override
    public void onNewIntent(Intent intent) {
		if(mInReadMode) {
			mInReadMode = false;
			
			// reads scanned tag
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			try {
				readTag(tag);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void enableReadMode() {
		mInReadMode = true;
		
		// set up a PendingIntent to open the app when a tag is scanned
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] filters = new IntentFilter[] { tagDetected };
        
		mAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
	}
	
	private void disableWriteMode() {
		mAdapter.disableForegroundDispatch(this);
	}
	
	private void readTag(Tag tag) throws IOException {
		
		//  3) Get an instance of the TAG from the NfcAdapter
            //Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            
            // 4) Get an instance of the Mifare classic card from this TAG intent
            mfc = MifareClassic.get(tag);

            //  5.1) Connect to card             
            try {
				mfc.connect();
				
            boolean auth = false;
            byte keya[] = { (byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF};

            if(!default_key){
           
            	String key = getKey();
            	key = toHex(key).toUpperCase();
            	int len = key.length();
            	key = key.substring(28,40);
            	
            	keya = new BigInteger(key, 16).toByteArray();
            	
            	//Toast.makeText(getApplicationContext(), ""+keya, Toast.LENGTH_LONG).show();
            }
           
            auth = mfc.authenticateSectorWithKeyA(getSector(), keya);
            if(auth)
            {
           	Toast.makeText(getApplicationContext(), "Authenticated with KEY A successfully", Toast.LENGTH_LONG).show();
           	mReadSectorButton.setVisibility(View.VISIBLE);
            }
            else
            {
            	Toast.makeText(getApplicationContext(), "Authentication unsuccessful", Toast.LENGTH_LONG).show();
            }
            /*
            byte[] keya = { (byte) 0x75,(byte) 0x6E,(byte) 0x6C,(byte) 0x6F,(byte) 0x63,(byte) 0x6B};
             
            auth = mfc.authenticateSectorWithKeyA(getBlock()/4, keya);
             if(auth)
             {
            	Toast.makeText(getApplicationContext(), "Authenticated with KEY A successfully", Toast.LENGTH_LONG).show();
            	
            	
            	String s = getData();
            	s = toHex(s);
             	BigInteger bi = new BigInteger(s, 16);
             	byte[] a1 = bi.toByteArray();
             	byte[] a2 = new byte[16];
             	System.arraycopy(a1, 0, a2, 16 - a1.length, a1.length);
             	mfc.writeBlock(getBlock(),a2); 
             	
            
             	displayMessage("Tag written successfully");
             }
             else
            	 displayMessage("Authentication failed with KeyA");
            
             */
             
            }catch (IOException e) { 
                Log.e(TAG, e.getLocalizedMessage());
                //showAlert(3);
            }
            
	    }
		
		private int getSector() {
			 String s = mSector.getText().toString();
			 return Integer.parseInt(s);
		}
		
		private String getKey() {
			 return mKey.getText().toString();
		}
		
		public String toHex(String arg) {
		    return String.format("%040x", new BigInteger(arg.getBytes()));
		}
		
		public void addListenerOnmDefault() {
			 
			mDefault = (CheckBox) findViewById(R.id.chkDefault);
		 
			mDefault.setOnClickListener(new OnClickListener() {
		 
			  @Override
			  public void onClick(View v) {
		                //is chkIos checked?
				if (((CheckBox) v).isChecked()) {
					default_key=true;
				}
		 
			  }
			});
		 
		  }
		
		private static String hex(String  binStr) {

	        String newStr = new String();

	        try {
	            String hexStr = "0123456789ABCDEF";
	            byte [] p = binStr.getBytes();
	            for(int k=0; k < p.length; k++ ){
	                int j = ( p[k] >> 4 )&0xF;
	                newStr = newStr + hexStr.charAt( j );
	                j = p[k]&0xF;
	                newStr = newStr + hexStr.charAt( j ) + " ";
	            }   
	        } catch (Exception e) {
	            System.out.println("Failed to convert into hex values: " + e);
	        } 

	        return newStr;
	    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
