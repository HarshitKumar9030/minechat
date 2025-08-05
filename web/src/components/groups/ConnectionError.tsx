import React from 'react';
import { AlertTriangle, Wifi, RefreshCw } from 'lucide-react';

interface ConnectionErrorProps {
  onRetry?: () => void;
  isRetrying?: boolean;
}

const ConnectionError: React.FC<ConnectionErrorProps> = ({
  onRetry,
  isRetrying = false
}) => {
  return (
    <div className="flex items-center justify-center min-h-[400px]">
      <div className="text-center max-w-md mx-auto p-8">
        <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <AlertTriangle className="w-8 h-8 text-red-600" />
        </div>
        
        <h3 className="text-xl font-bold text-neutral-100 mb-2 font-minecraftia">
          Connection Error
        </h3>
        
        <p className="text-neutral-400 mb-6">
          Unable to connect to the server. Please check your internet connection and try again.
        </p>
        
        <div className="space-y-3">
          <div className="flex items-center justify-center gap-2 text-sm text-neutral-500">
            <Wifi className="w-4 h-4" />
            <span>Check your network connection</span>
          </div>
          
          {onRetry && (
            <button
              onClick={onRetry}
              disabled={isRetrying}
              className="px-6 py-2 bg-yellow-600 hover:bg-yellow-700 disabled:bg-neutral-600 disabled:cursor-not-allowed text-neutral-900 disabled:text-neutral-400 rounded-lg transition-colors flex items-center gap-2 mx-auto"
            >
              {isRetrying ? (
                <>
                  <div className="w-4 h-4 border-2 border-neutral-400 border-t-transparent rounded-full animate-spin"></div>
                  Retrying...
                </>
              ) : (
                <>
                  <RefreshCw className="w-4 h-4" />
                  Try Again
                </>
              )}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default ConnectionError;
